package org.iplantc.service.profile.dao;

/**
 * @author dooley
 *
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.iplantc.service.profile.Settings;
import org.iplantc.service.profile.exceptions.RemoteDataException;
import org.iplantc.service.profile.model.Profile;
import org.iplantc.service.profile.model.TrellisDatabaseProfile;

public class XSEDEProfileDAO extends AbstractProfileDAO {
    @SuppressWarnings("unused")
	private Logger log = LogManager.getLogger(XSEDEProfileDAO.class);
    
    private String baseQuery = "SELECT p.id as id, " + 
											 	"p.gender as gender,  " + 
											    "p.first_name as first_name,  " + 
											    "p.last_name as last_name, " + 
											    "pr.department as department,  " + 
											    "pr.position as position, " + 
											    "acc.username as username, " + 
											    "e.email as email, " + 
											    "ph.number as phone, " + 
											    "f.number as fax, " + 
											    "a.country as country, " + 
											    "a.state as state, " + 
											    "a.city as city, " + 
											    "i.name as institution, " + 
											    "eth.name as ethnicity, " + 
											    "ra.name as research_area " + 
									    "FROM people p  " + 
											    "left join profiles pr using(id) " + 
											    "left join accounts acc using(id) " + 
											    "left outer join emails e on p.id = e.person_id " + 
											    "left outer join phones ph on p.id = ph.person_id " + 
											    "left outer join faxes f on p.id = f.person_id " + 
											    "left outer join addresses a on p.id = a.person_id " + 
											    "left outer join institutions as i on pr.institution_id = i.id " + 
											    "left outer join ethnicities as eth on p.ethnicity_id = eth.id  " + 
											    "left outer join research_areas as ra on pr.research_area_id = ra.id ";
    
    // ********************************************************** //
    
    public XSEDEProfileDAO() {}
    
    private Connection getConnection() throws NamingException, SQLException, ClassNotFoundException {
		Connection connection = null;
    	if (Settings.USE_SSH_TUNNEL)
    	{
    		Properties connectionProps = new Properties();
            connectionProps.put("user", Settings.DB_USERNAME);
            connectionProps.put("password", Settings.DB_PASSWORD);
            
            String connectionString = "jdbc:mysql://localhost/" + Settings.DB_NAME + "?"
                + "socketFactory=SSHSocketFactory"
                + "&SSHHost="+Settings.SSH_TUNNEL_HOST
            	+ "&SSHUser=" + Settings.SSH_TUNNEL_USERNAME
            	+ "&SSHPassword="+Settings.SSH_TUNNEL_PASSWORD
            	+ "&SSHPort=" + Settings.SSH_TUNNEL_PORT
            	+ "&user=" + Settings.DB_USERNAME
            	+ "&password=" + Settings.DB_PASSWORD;
            
            connection = DriverManager.getConnection(connectionString);
            
    	}
    	else // use jndi
    	{
	    	Context context = new InitialContext();
			
			DataSource ds = (DataSource) context.lookup("java:/comp/env/jdbc/trellis");
			
			if (ds != null) {
	            connection = ds.getConnection();
	        }
    	}
    	
    	return connection;
	}
    
    /* (non-Javadoc)
     * @see org.iplantc.service.profile.dao.ProfileDAO#getByUsername(java.lang.String)
     */
    public Profile getByUsername(String username) throws RemoteDataException {
        String sql = baseQuery + "WHERE acc.username = '" + username + "' GROUP BY acc.username ORDER BY p.last_name ASC";
        return runQuery(sql).get(0);
    }
    
    /* (non-Javadoc)
	 * @see org.iplantc.service.profile.dao.ProfileDAO#searchByUsername(java.lang.String)
	 */
    public List<Profile> searchByUsername(String username) throws RemoteDataException 
    {
        String sql = baseQuery + "WHERE acc.username like '%" + username + "%' GROUP BY acc.username ORDER BY p.last_name ASC";
        return runQuery(sql);
    }
    
    /* (non-Javadoc)
	 * @see org.iplantc.service.profile.dao.ProfileDAO#searchByEmail(java.lang.String)
	 */
    public List<Profile> searchByEmail(String email) throws RemoteDataException 
    {
        String sql = baseQuery + "WHERE e.email like '%" + email + "%' GROUP BY acc.username ORDER BY p.last_name ASC";
        return runQuery(sql);
    }
    
    /* (non-Javadoc)
	 * @see org.iplantc.service.profile.dao.ProfileDAO#searchByFullName(java.lang.String)
	 */
    public List<Profile> searchByFullName(String name) throws RemoteDataException 
    {
        String sql = baseQuery + "WHERE concat(p.first_name, ' ', p.last_name) like '%" + name + "%' GROUP BY acc.username ORDER BY p.last_name ASC";
        return runQuery(sql);
    }
    
    private List<Profile> runQuery(String sql) throws RemoteDataException 
    {
    	List<Profile> profiles = new ArrayList<Profile>();
    	PreparedStatement ps = null;
    	ResultSet rs = null;
    	Connection conn = null;
    	
    	try 
    	{
    		conn = getConnection();
	        
	        if (conn == null) {
	        	return null;
	        }
	        
        	ps = conn.prepareStatement(sql);
        	ps.executeQuery();
        	rs = ps.getResultSet();
        	
            while (rs.next()) {
            	Profile profile = new TrellisDatabaseProfile(rs);
            	profiles.add(profile);
            }
        } 
    	catch (Exception ex) {
			throw new RemoteDataException("Failed to query remote profile database", ex);
		} 
    	finally {
			try { rs.close(); } catch (Exception e) {}
            try { ps.close(); } catch (Exception e) {}
            try { conn.close(); } catch (Exception e) {}
        }
        
        return profiles;
        
	}
}