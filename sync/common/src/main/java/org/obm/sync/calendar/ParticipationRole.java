package org.obm.sync.calendar;

import java.lang.reflect.Method;

import org.obm.sync.base.ObmDbType;

public enum ParticipationRole {

	CHAIR, REQ, OPT, NON;
	public Object getJdbcObject(ObmDbType type) {
		if (type == ObmDbType.PGSQL) {
			try {
				Object o = Class.forName("org.postgresql.util.PGobject")
						.newInstance();
				Method setType = o.getClass()
						.getMethod("setType", String.class);
				Method setValue = o.getClass().getMethod("setValue",
						String.class);

				setType.invoke(o, "vrole");
				setValue.invoke(o, toString());
				return o;
			} catch (Throwable e) {
				e.printStackTrace();
			}
			return null;
		}
		return toString();
	}
}
