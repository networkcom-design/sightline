-- Los cambios que el auditor le hace al presupuesto de una auditoria concreta:
-- que servicios entran y a que precio.
--
-- Va como JSON en una columna y no en una tabla aparte porque se lee y se
-- escribe siempre entero, junto con la auditoria, y nunca se consulta ni se
-- ordena por sus campos. Una tabla con una fila por servicio agregaria joins
-- y migraciones cada vez que cambie el catalogo, sin ganar nada.
ALTER TABLE auditorias ADD COLUMN ajustes_propuesta TEXT;
