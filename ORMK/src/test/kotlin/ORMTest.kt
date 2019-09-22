import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class ORMTest {

    @BeforeEach
    fun before() {
        CrudRepsitory.execute("drop schema if exists test CASCADE")
        CrudRepsitory.execute("create schema if not exists test")
    }

    @Test
    fun `test execute with query resultset impl`() {
        //create table
        val createTableSQL = "create table test.DummyModel (data VARCHAR, anint INT)"
        CrudRepsitory.execute(createTableSQL)
        val tableName = CrudRepsitory.query("show tables from test").apply { first() }.getString("TABLE_NAME")
        assertThat(tableName).isEqualToIgnoringCase("DummyModel")
    }

    @Test
    fun `test ORM Save and Map model`() {
        @Schema("test")
        data class DummyModel(val data: String, val anint: Int)

        CrudRepsitory.save(DummyModel("a dummy data", 100), true)

        val query = CrudRepsitory.query<DummyModel>("Select * from test.DummyModel", DummyModel::class.java).first()

        assertThat(query.data.equals("a dummy data"))
        assertThat(query.anint == 100)
    }


    @Test
    fun `create table test`() {
        //define model
        @Schema("test")
        data class DummyModel(@Id val name: String, @Indexed val data: String, val anint: Int)
        CrudRepsitory.createTable(DummyModel::class.java)
    }

    @Test
    fun `create table with composit pk`() {
        //define model
        @Schema("test")
        data class DummyModel(@Id val name: String, @Indexed val data: String, @Id val anint: Int, val dob: Instant)
        CrudRepsitory.createTable(DummyModel::class.java)
    }


}
