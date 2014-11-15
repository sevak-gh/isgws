package com.infotech.isg.repository.jdbc;

import com.infotech.isg.domain.Client;
import com.infotech.isg.repository.ClientRepository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.dao.EmptyResultDataAccessException;


/**
* jdbc implementation of Client repository
*
* @author Sevak Gharibian
*/
@Repository("ClientRepositoryJdbc")
public class ClientRepositoryImpl implements ClientRepository {
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Client findByUsername(String username) {
        Client client = null;
        String sql = "select id, client, pin, active from info_topup_clients where client = ?";
        try {
            client = jdbcTemplate.queryForObject(sql, new Object[] {username}, new ClientRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }

        sql = "select ip from info_topup_client_ips where client = ?";
        List<String> ips = jdbcTemplate.queryForList(sql, new Object[] {client.getId()}, String.class);
        for (String ip : ips) {
            client.addIp(ip);
        }

        return client;
    }

    private static final class ClientRowMapper implements RowMapper<Client> {
        @Override
        public Client mapRow(ResultSet rs, int rowNum) throws SQLException {
            Client client = new Client();
            client.setId(rs.getInt("id"));
            client.setUsername(rs.getString("client"));
            client.setPassword(rs.getString("pin"));
            client.setIsActive(((rs.getString("active").compareToIgnoreCase("Y") == 0) ? true : false));
            return client;
        }
    }
}