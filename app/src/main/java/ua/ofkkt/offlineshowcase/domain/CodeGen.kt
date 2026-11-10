package ua.ofkkt.offlineshowcase.domain

data class FieldSpec(val name: String, val type: String)

/** "name:String, price:Double" → [FieldSpec]. Тип за замовчуванням — String. */
fun parseFields(spec: String): List<FieldSpec> =
    spec.split(',', ';', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { part ->
            val pieces = part.split(':', ' ').map { it.trim() }.filter { it.isNotEmpty() }
            val name = pieces.firstOrNull()?.filter { it.isLetterOrDigit() || it == '_' }.orEmpty()
            if (name.isEmpty() || name.equals("id", ignoreCase = true)) {
                null
            } else {
                val type = pieces.getOrNull(1)?.replaceFirstChar { it.uppercase() } ?: "String"
                FieldSpec(name, type)
            }
        }

fun snakeCase(text: String): String =
    text.fold(StringBuilder()) { acc, ch ->
        if (ch.isUpperCase() && acc.isNotEmpty()) acc.append('_')
        acc.append(ch.lowercaseChar())
    }.toString()

/** Слайд 17: каркас «Entity + DAO + Repository» для власної сутності студента. */
fun generateRoomSkeleton(entityName: String, fields: List<FieldSpec>): String {
    val clean = entityName.filter { it.isLetterOrDigit() }.replaceFirstChar { it.uppercase() }
    val name = clean.ifEmpty { "Item" }
    val table = snakeCase(name) + "s"
    val entityFields = fields.joinToString("\n") { "    val ${it.name}: ${it.type}," }

    return buildString {
        appendLine("@Entity(tableName = \"$table\")")
        appendLine("data class ${name}Entity(")
        appendLine("    @PrimaryKey val id: Int,")
        if (entityFields.isNotEmpty()) appendLine(entityFields)
        appendLine("    val cachedAt: Long = System.currentTimeMillis(),")
        appendLine(")")
        appendLine()
        appendLine("@Dao")
        appendLine("interface ${name}Dao {")
        appendLine("    @Query(\"SELECT * FROM $table ORDER BY id\")")
        appendLine("    fun getAll(): Flow<List<${name}Entity>>")
        appendLine()
        appendLine("    @Insert(onConflict = OnConflictStrategy.REPLACE)")
        appendLine("    suspend fun insertAll(items: List<${name}Entity>)")
        appendLine()
        appendLine("    @Query(\"DELETE FROM $table\")")
        appendLine("    suspend fun deleteAll()")
        appendLine("}")
        appendLine()
        appendLine("class ${name}Repository(private val dao: ${name}Dao, private val api: ${name}Api) {")
        appendLine("    fun getItems(): Flow<List<$name>> =")
        appendLine("        dao.getAll().map { list -> list.map { it.toDomain() } }")
        appendLine()
        appendLine("    suspend fun refresh() {")
        appendLine("        try {")
        appendLine("            val remote = api.getItems()")
        appendLine("            dao.deleteAll()")
        appendLine("            dao.insertAll(remote.map { it.toEntity() })")
        appendLine("        } catch (e: IOException) {")
        appendLine("            // мережа недоступна — локальні дані лишаються")
        appendLine("        }")
        appendLine("    }")
        append("}")
    }
}
