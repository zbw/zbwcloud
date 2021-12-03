package de.zbw.persistence.access.server

import de.zbw.api.access.server.config.AccessConfiguration
import de.zbw.business.access.server.Action
import de.zbw.business.access.server.ActionType
import de.zbw.business.access.server.Attribute
import de.zbw.business.access.server.AttributeType
import de.zbw.business.access.server.Header
import de.zbw.business.access.server.Restriction
import de.zbw.business.access.server.RestrictionType
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Types

/**
 * Connector for interacting with the postgres database.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class DatabaseConnector(
    internal val connection: Connection,
) {

    constructor(
        config: AccessConfiguration,
    ) : this(DriverManager.getConnection(config.sqlUrl, config.sqlUser, config.sqlPassword))

    fun insertHeader(header: Header): String {
        val stmntAccIns =
            "INSERT INTO $TABLE_NAME_HEADER" +
                "(header_id,tenant,usage_guide,template,mention,sharealike,commercial_use,copyright) " +
                "VALUES(?,?,?,?,?,?,?,?)"

        val prepStmt = connection.prepareStatement(stmntAccIns, Statement.RETURN_GENERATED_KEYS).apply {
            this.setString(1, header.id)
            this.setIfNotNull(2, header.tenant) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(3, header.usageGuide) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(4, header.template) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setBoolean(5, header.mention)
            this.setBoolean(6, header.shareAlike)
            this.setBoolean(7, header.commercialUse)
            this.setBoolean(8, header.copyright)
        }
        val affectedRows = prepStmt.run { this.executeUpdate() }

        return if (affectedRows > 0) {
            val rs: ResultSet = prepStmt.generatedKeys
            rs.next()
            rs.getString(1)
        } else throw IllegalStateException("No row has been inserted.")
    }

    fun insertAction(action: Action, fkAccessRight: String): Long {
        val stmntActIns = "INSERT INTO $TABLE_NAME_ACTION" +
            "(type, permission, header_id) " +
            "VALUES(?,?,?)"
        val prepStmt = connection.prepareStatement(stmntActIns, Statement.RETURN_GENERATED_KEYS).apply {
            this.setString(1, action.type.toString())
            this.setBoolean(2, action.permission)
            this.setString(3, fkAccessRight)
        }
        val affectedRows = prepStmt.run { this.executeUpdate() }
        return if (affectedRows > 0) {
            val rs: ResultSet = prepStmt.generatedKeys
            rs.next()
            rs.getLong(1)
        } else throw IllegalStateException("No row has been inserted.")
    }

    fun insertRestriction(restriction: Restriction, fkActionId: Long): Long {
        val stmntRestIns = "INSERT INTO $TABLE_NAME_RESTRICTION" +
            "(type, attribute_type, attribute_values, action_id) " +
            "VALUES(?,?,?,?)"
        val prepStmt = connection.prepareStatement(stmntRestIns, Statement.RETURN_GENERATED_KEYS).apply {
            this.setString(1, restriction.type.toString())
            this.setString(2, restriction.attribute.type.name)
            this.setString(3, restriction.attribute.values.joinToString(separator = ";"))
            this.setLong(4, fkActionId)
        }
        val affectedRows = prepStmt.executeUpdate()
        return if (affectedRows > 0) {
            val rs: ResultSet = prepStmt.generatedKeys
            rs.next()
            rs.getLong(1)
        } else throw IllegalStateException("No row has been inserted.")
    }

    fun getHeaders(headerIds: List<String>): List<Header> {
        val stmt =
            "SELECT header_id, tenant, usage_guide, template, mention, sharealike, commercial_use, copyright " +
                "FROM $TABLE_NAME_HEADER " +
                "WHERE header_id = ANY(?)"

        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setArray(1, connection.createArrayOf("text", headerIds.toTypedArray()))
        }
        val rs = prepStmt.executeQuery()
        return generateSequence {
            if (rs.next()) {
                Header(
                    rs.getString(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getBoolean(5),
                    rs.getBoolean(6),
                    rs.getBoolean(7),
                    rs.getBoolean(8),
                )
            } else null
        }.takeWhile { true }.toList()
    }

    fun deleteAccessRights(headerIds: List<String>): Int {
        val keys = getAccessInformationKeys(headerIds)
        val existingHeaderIds = keys.map { it.headerId }
        val actionIds = keys.map { it.actionId }
        val restrictionIds = keys.map { it.restrictionId }

        deleteRestrictions(restrictionIds)
        deleteActions(actionIds)
        return deleteHeader(existingHeaderIds)
    }

    internal fun getAccessInformationKeys(headerIds: List<String>): List<JoinHeaderActionRestrictionIdTransient> {
        // First, receive the required primary keys.
        val stmt =
            "SELECT a.header_id, a.action_id, r.restriction_id " +
                "FROM $TABLE_NAME_ACTION a " +
                "LEFT JOIN $TABLE_NAME_RESTRICTION r ON a.action_id = r.action_id " +
                "WHERE a.header_id = ANY(?)"
        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setArray(1, connection.createArrayOf("text", headerIds.toTypedArray()))
        }
        val rs = prepStmt.executeQuery()
        return generateSequence {
            if (rs.next()) {
                JoinHeaderActionRestrictionIdTransient(
                    headerId = rs.getString(1),
                    actionId = rs.getInt(2),
                    restrictionId = rs.getInt(3)
                )
            } else null
        }.takeWhile { true }.toList()
    }

    private fun deleteRestrictions(restrictionIds: List<Int>): Int {
        val stmt =
            "DELETE " +
                "FROM $TABLE_NAME_RESTRICTION r " +
                "WHERE r.restriction_id = ANY(?)"
        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setArray(1, connection.createArrayOf("integer", restrictionIds.toTypedArray()))
        }
        return prepStmt.run { this.executeUpdate() }
    }

    private fun deleteActions(actionIds: List<Int>): Int {
        val stmt =
            "DELETE " +
                "FROM $TABLE_NAME_ACTION a " +
                "WHERE a.action_id = ANY(?)"
        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setArray(1, connection.createArrayOf("integer", actionIds.toTypedArray()))
        }
        return prepStmt.run { this.executeUpdate() }
    }

    private fun deleteHeader(headerIds: List<String>): Int {
        val stmt =
            "DELETE " +
                "FROM $TABLE_NAME_HEADER h " +
                "WHERE h.header_id = ANY(?)"
        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setArray(1, connection.createArrayOf("text", headerIds.toTypedArray()))
        }
        return prepStmt.run { this.executeUpdate() }
    }

    fun getActions(headerIds: List<String>): Map<String, List<Action>> {
        val stmt =
            "SELECT a.header_id, a.type, a.permission, r.type, r.attribute_type, r.attribute_values " +
                "FROM $TABLE_NAME_ACTION a " +
                "LEFT JOIN $TABLE_NAME_RESTRICTION r ON a.action_id = r.action_id " +
                "WHERE a.header_id = ANY(?)"

        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setArray(1, connection.createArrayOf("text", headerIds.toTypedArray()))
        }
        val rs = prepStmt.executeQuery()
        val joinResult: List<JoinActionRestrictionTransient> = generateSequence {
            if (rs.next()) {
                JoinActionRestrictionTransient(
                    headerId = rs.getString(1),
                    actionType = rs.getString(2),
                    actionPermission = rs.getBoolean(3),
                    restrictionType = rs.getObject(4) as String?,
                    attributeType = rs.getObject(5) as String?,
                    attributeValues = rs.getObject(6) as String?,
                )
            } else null
        }.takeWhile { true }.toList()
        return joinResult.groupBy { it.headerId }.map { entry ->
            Pair(
                entry.key,
                entry.value.groupBy { it.actionType }.map { actionLvl ->
                    Action(
                        type = ActionType.valueOf(actionLvl.key),
                        permission = actionLvl.value.first().actionPermission,
                        restrictions = actionLvl.value.filter { it.restrictionType != null && it.attributeType != null && it.attributeValues != null }
                            .map { restrictionLvl ->
                                Restriction(
                                    type = RestrictionType.valueOf(restrictionLvl.restrictionType!!),
                                    attribute = Attribute(
                                        type = AttributeType.valueOf(restrictionLvl.attributeType!!),
                                        values = restrictionLvl.attributeValues!!.split(","),
                                    )
                                )
                            }
                    )
                }
            )
        }.toMap()
    }

    fun containsHeader(headerId: String): Boolean {
        val stmt = "SELECT EXISTS(SELECT 1 from $TABLE_NAME_HEADER WHERE header_id=?)"
        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setString(1, headerId)
        }
        val rs = prepStmt.executeQuery()
        rs.next()
        return rs.getBoolean(1)
    }

    fun getAccessRightIds(limit: Int, offset: Int): List<String> {
        val stmt = "SELECT header_id from $TABLE_NAME_HEADER ORDER BY header_id ASC LIMIT ? OFFSET ?"
        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setInt(1, limit)
            this.setInt(2, offset)
        }
        val rs = prepStmt.executeQuery()

        return generateSequence {
            if (rs.next()) {
                rs.getString(1)
            } else null
        }.takeWhile { true }.toList()
    }

    private fun <T> PreparedStatement.setIfNotNull(
        idx: Int,
        element: T?,
        setter: (T, Int, PreparedStatement) -> Unit,
    ) = element?.let { setter(element, idx, this) } ?: this.setNull(idx, Types.NULL)

    companion object {
        const val TABLE_NAME_HEADER = "access_right_header"
        const val TABLE_NAME_ACTION = "access_right_action"
        const val TABLE_NAME_RESTRICTION = "access_right_restriction"
    }
}