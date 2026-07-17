package com.hanserdev.interview.domain.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

/**
 * 统一处理 PostgreSQL 中 uuid -> Java UUID 的读写映射
 */
@MappedTypes(UUID.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class PostgresUUIDTypeHandler extends BaseTypeHandler<UUID> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, parameter, Types.OTHER);
    }

    @Override
    public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return convert(rs.getObject(columnName));
    }

    @Override
    public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return convert(rs.getObject(columnIndex));
    }

    @Override
    public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return convert(cs.getObject(columnIndex));
    }

    private UUID convert(Object source) throws SQLException {
        if (source == null) {
            return null;
        }
        if (source instanceof UUID uuid) {
            return uuid;
        }
        if (source instanceof PGobject pgObject) {
            String value = pgObject.getValue();
            return value == null ? null : UUID.fromString(value);
        }
        if (source instanceof String str && !str.isBlank()) {
            return UUID.fromString(str);
        }
        throw new SQLException("无法将值转换为UUID：" + source);
    }
}
