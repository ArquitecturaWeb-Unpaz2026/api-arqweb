-- Baja lógica de usuarios, el mismo criterio que tareas.fechaeliminacion.
-- Hibernate no altera el esquema (ddl-auto=none): esta columna se agrega a mano.
-- Idempotente para poder correrlo más de una vez.

SET @usuarios_fechaeliminacion := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'usuarios'
      AND COLUMN_NAME = 'fechaeliminacion'
);
SET @sql_usuarios_fechaeliminacion := IF(
    @usuarios_fechaeliminacion = 0,
    'ALTER TABLE usuarios ADD COLUMN fechaeliminacion timestamp NULL',
    'SELECT 1'
);
PREPARE stmt_usuarios_fechaeliminacion FROM @sql_usuarios_fechaeliminacion;
EXECUTE stmt_usuarios_fechaeliminacion;
DEALLOCATE PREPARE stmt_usuarios_fechaeliminacion;
