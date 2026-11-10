package ua.ofkkt.offlineshowcase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.ofkkt.offlineshowcase.domain.FieldSpec
import ua.ofkkt.offlineshowcase.domain.generateRoomSkeleton
import ua.ofkkt.offlineshowcase.domain.parseFields
import ua.ofkkt.offlineshowcase.domain.snakeCase

class CodeGenTest {

    @Test
    fun parsesNameAndType() {
        val fields = parseFields("title:String, pages:int, price Double")
        assertEquals(
            listOf(FieldSpec("title", "String"), FieldSpec("pages", "Int"), FieldSpec("price", "Double")),
            fields,
        )
    }

    @Test
    fun defaultsToStringAndSkipsId() {
        assertEquals(listOf(FieldSpec("author", "String")), parseFields("id:Int, author"))
    }

    @Test
    fun snakeCaseConvertsCamelCase() {
        assertEquals("product_item", snakeCase("ProductItem"))
        assertEquals("book", snakeCase("Book"))
    }

    @Test
    fun skeletonContainsEntityDaoAndRepository() {
        val code = generateRoomSkeleton("book", parseFields("title:String, pages:Int"))
        assertTrue(code.contains("@Entity(tableName = \"books\")"))
        assertTrue(code.contains("data class BookEntity("))
        assertTrue(code.contains("val title: String,"))
        assertTrue(code.contains("val pages: Int,"))
        assertTrue(code.contains("interface BookDao"))
        assertTrue(code.contains("class BookRepository"))
    }

    @Test
    fun emptyNameFallsBackToItem() {
        assertTrue(generateRoomSkeleton("  ", emptyList()).contains("ItemEntity"))
    }
}
