package com.forj.fwm.backend.dao;

import java.sql.SQLException;
import java.util.List;

import com.forj.fwm.entity.Statblock;
import com.j256.ormlite.dao.Dao;

public interface StatblockDao extends Dao<Statblock,String> {
	public List<Statblock> queryForLike(String arg0, Object arg1) throws SQLException;
}
