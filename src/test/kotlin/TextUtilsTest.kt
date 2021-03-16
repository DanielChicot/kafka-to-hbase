import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.ExperimentalTime

@ExperimentalTime
class TextUtilsTest : StringSpec({

    fun reset() {
        Config.Hbase.qualifiedTablePattern = Config.Hbase.DEFAULT_QUALIFIED_TABLE_PATTERN
    }

    "table names will have dots and dashes replaced" {
        val actual = TextUtils().targetTable("a.b-c", "d.e-f")
        actual shouldBe "a_b_c:d_e_f"
    }

    "table names will have dots and dashes replaced in edge case topic names" {
        val actual = TextUtils().targetTable("ucfs", "data.encrypted")
        actual shouldBe "ucfs:data_encrypted"
    }

    "agent-core:agentToDoArchive is coalesced." {
        val actual = TextUtils().coalescedName("agent-core:agentToDoArchive")
        actual shouldBe "agent-core:agentToDo"
    }

    "other_db:agentToDoArchive is not coalesced." {
        val actual = TextUtils().coalescedName("other_db:agentToDoArchive")
        actual shouldBe "other_db:agentToDoArchive"
    }

    "Not agentToDoArchive is not coalesced." {
        val actual = TextUtils().coalescedName("core:calculationParts")
        actual shouldBe "core:calculationParts"
    }

    "Test topic name table matcher will use ucfs data feed regex to match against valid table name" {

        val tableName = "db.ucfs.data"

        Config.Hbase.qualifiedTablePattern = Config.Hbase.DEFAULT_QUALIFIED_TABLE_PATTERN

        val result = TextUtils().topicNameTableMatcher(tableName)

        result shouldNotBe null

        assert(result!!.groupValues[1] == "ucfs")
        assert(result.groupValues[2] == "data")
        reset()
    }

    "Test topic name table matcher will use ucfs data feed regex to match against valid table name with extra stanza" {
        // extra stanza test for UC edge cases in topic names

        val tableName = "db.ucfs.data.encrypted"

        Config.Hbase.qualifiedTablePattern = Config.Hbase.DEFAULT_QUALIFIED_TABLE_PATTERN

        val result = TextUtils().topicNameTableMatcher(tableName)

        result shouldNotBe null

        assert(result!!.groupValues[1] == "ucfs")
        assert(result.groupValues[2] == "data.encrypted")
        reset()
    }

    "Test topic name table matcher will use data equalities regex to match against valid table name" {

        val tableName = "data.equality_1324324234"

        Config.Hbase.qualifiedTablePattern = """([-\w]+)\.([-\w]+)"""

        val result: MatchResult? = TextUtils().topicNameTableMatcher(tableName)

        result shouldNotBe null

        assert(result!!.groupValues[1] == "data")
        assert(result.groupValues[2] == "equality_1324324234")
        reset()
    }
})
