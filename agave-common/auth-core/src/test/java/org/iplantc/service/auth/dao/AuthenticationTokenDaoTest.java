/**
 * 
 */
package org.iplantc.service.auth.dao;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.iplantc.service.auth.model.AuthenticationToken;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(groups={"integration"})
public class AuthenticationTokenDaoTest {

	private static final String INTERNAL_USER_CREATOR = "testuser";
	private static final String INTERNAL_USER_STRANGER = "bob";
    
    private AuthenticationTokenDao dao;
	/**
	 * Initalizes the test db and adds the test app 
	 */
	@BeforeClass
	public void initDb()
	{
		try
		{
			dao = new AuthenticationTokenDao();
			
			for(AuthenticationToken token: dao.findAllByUsername(INTERNAL_USER_CREATOR)) {
				dao.delete(token);
			}
		}
		catch (Exception e)
		{	
			e.printStackTrace();
		}
	}
	
	private AuthenticationToken createAuthenticationToken() throws Exception {
	
		AuthenticationToken token = new AuthenticationToken(INTERNAL_USER_CREATOR);
		token.setInternalUsername("testuser");
		return token;
	}
	
	@DataProvider(name="persistenceAuthenticationTokenProvider")
	public Object[][] persistenceAuthenticationTokenProvider() throws Exception
	{
		return new Object[][] {
			{ createAuthenticationToken(), "Authentication token will persist", false }
		};
	}
	
	@Test (dataProvider="persistenceAuthenticationTokenProvider")
	public void persist(AuthenticationToken token, String message, Boolean shouldThrowException) throws Exception {
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		try 
		{
			dao.persist(token);
			Assert.assertNotNull(token.getId(), "Failed to generate a token ID.");
		} 
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Error persisting token " + token.getUsername() + ": " + message;
            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
		}
		finally
		{
			try { dao.delete(token); } catch (Exception e) {}
		}
		
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
	}
	
	@DataProvider(name="updateAuthenticationTokenProvider")
	public Object[][] updateProvider() throws Exception
	{
		return new Object[][] {
			{ createAuthenticationToken(), "AuthenticationToken will update", false }
		};
	}
	
	@Test (dataProvider="updateAuthenticationTokenProvider")
	public void update(AuthenticationToken token, String message, Boolean shouldThrowException) throws Exception {
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		try 
		{
			dao.persist(token);

			Assert.assertNotNull(token.getId(), "AuthenticationToken got an id after persisting.");
			
			AuthenticationToken savedAuthenticationToken = (AuthenticationToken)dao.findById(token.getId());
			
			Assert.assertNotNull(savedAuthenticationToken, "AuthenticationToken was found in db.");
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MONTH, 1);
			
			savedAuthenticationToken.setExpirationDate(cal.getTime());
			
			dao.persist(savedAuthenticationToken);
			
			AuthenticationToken updatedAuthenticationToken = (AuthenticationToken)dao.findById(token.getId());
			
			Assert.assertNotNull(updatedAuthenticationToken, "AuthenticationToken was found in db.");
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String currentExpires = formatter.format(updatedAuthenticationToken.getExpirationDate());
			String calValue = formatter.format(cal.getTime());
			Assert.assertEquals(currentExpires, calValue, "Failed to updated the expiration date.");
		} 
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Error persisting token " + token.getUsername() + ": " + message;
            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
		}
		finally
		{
			try { dao.delete(token); } catch (Exception e) {}
		}
		
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
	}
	
	@DataProvider(name="deleteAuthenticationTokenProvider")
	public Object[][] deleteAuthenticationTokenProvider() throws Exception
	{
		return new Object[][] {
			{ createAuthenticationToken(), "AuthenticationToken will delete", false }
		};
	}
	
	@Test (dataProvider="deleteAuthenticationTokenProvider")
	public void delete(AuthenticationToken token, String message, Boolean shouldThrowException) 
	throws Exception 
	{
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		
		try 
		{
			dao.persist(token);

			Assert.assertNotNull(token.getId(), "AuthenticationToken got an id after persisting.");
			
			Long id = token.getId();
			
			AuthenticationToken savedToken = dao.findById(id);
			
			Assert.assertNotNull(savedToken, "AuthenticationToken was found in db.");
			
			dao.delete(savedToken);
			
			AuthenticationToken deletedToken = dao.findById(id);
			
			Assert.assertNull(deletedToken, "AuthenticationToken was not found in db.");
		} 
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Error persisting token " + token.getUsername() + ": " + message;
            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
		}
		finally
		{
			try { dao.delete(token); } catch (Exception e) {}
		}
		
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
	}
	
	@DataProvider(name="findByExampleAuthenticationTokenProvider")
	public Object[][] findByExampleAuthenticationTokenProvider() throws Exception
	{
		return new Object[][] {
			{ createAuthenticationToken(), "AuthenticationToken was found by example", false }
		};
	}
	
	@Test (dataProvider="findByExampleAuthenticationTokenProvider")
	public void findByToken(AuthenticationToken token, String message, Boolean shouldThrowException) 
	throws Exception 
	{	
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		try 
		{
			dao.persist(token);
			
			Assert.assertNotNull(token.getId(), "AuthenticationToken got an id after persisting.");
			
			AuthenticationToken savedToken = dao.findByToken(token.getToken());
			
			Assert.assertNotNull(savedToken, "AuthenticationTokens were found matching example.");
			
			Assert.assertEquals(savedToken.getToken(), token.getToken(), "Exactly one match was found");
			
		} 
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Error persisting token " + token.getUsername() + ": " + message;
            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
		}
		finally
		{
			try { dao.delete(token); } catch (Exception e) {}
		}
		
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
	}
	
	@Test
	public void findAllByUsername() 
	{
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		AuthenticationToken token1 = null, token2 = null, token3 = null;
		
		try 
		{
			token1 = createAuthenticationToken();
			
			token2 = createAuthenticationToken();
			
			token3 = createAuthenticationToken();
			token3.setExpirationDate(new Date(System.currentTimeMillis() - 1000));
			
			dao.persist(token1);
			dao.persist(token2);
			dao.persist(token3);

			Assert.assertNotNull(token1.getId(), "First token failed to persist.");
			Assert.assertNotNull(token2.getId(), "Second token failed to persist.");
			Assert.assertNotNull(token3.getId(), "Third token failed to persist.");
			
			List<AuthenticationToken> tokens = dao.findAllByUsername(INTERNAL_USER_CREATOR);
			
			Assert.assertNotNull(tokens, "No internal tokens were found.");
			
			Assert.assertTrue(tokens.size() == 3, "No internal tokens were found.");
			
			Assert.assertTrue(tokens.contains(token1), "Results did not contain first token.");
			
			Assert.assertTrue(tokens.contains(token2), "Results did not contain second token");
			
			Assert.assertTrue(tokens.contains(token3), "Results contained inactive token");
			
		} 
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Failed to retrieve active tokens for " + INTERNAL_USER_CREATOR;
            e.printStackTrace();
		}
		finally
		{
			try { dao.delete(token1); } catch (Exception e) {}
			try { dao.delete(token2); } catch (Exception e) {}
			try { dao.delete(token3); } catch (Exception e) {}
		}
		
        System.out.println(" exception thrown?  expected false actual " + actuallyThrewException);
		Assert.assertFalse(actuallyThrewException, exceptionMsg);
	}

	@Test
	public void findActiveByUsername() 
	{
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		AuthenticationToken token1 = null, token2 = null, token3 = null;
		
		try 
		{
			token1 = createAuthenticationToken();
			
			token2 = createAuthenticationToken();
			
			token3 = createAuthenticationToken();
			token3.setExpirationDate(new Date(System.currentTimeMillis() - 10000));
			
			dao.persist(token1);
			dao.persist(token2);
			dao.persist(token3);

			Assert.assertNotNull(token1.getId(), "First token failed to persist.");
			Assert.assertNotNull(token2.getId(), "Second token failed to persist.");
			Assert.assertNotNull(token3.getId(), "Third token failed to persist.");
			
			List<AuthenticationToken> tokens = dao.findActiveByUsername(INTERNAL_USER_CREATOR);
			
			Assert.assertNotNull(tokens, "No internal tokens were found.");
			
			Assert.assertTrue(tokens.size() == 2, "No internal tokens were found.");
			
			Assert.assertTrue(tokens.contains(token1), "Results did not contain first token.");
			
			Assert.assertTrue(tokens.contains(token2), "Results did not contain second token");
			
			Assert.assertFalse(tokens.contains(token3), "Results contained inactive token");
			
		} 
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Failed to retrieve active tokens for " + INTERNAL_USER_CREATOR;
            e.printStackTrace();
		}
		finally
		{
			try { dao.delete(token1); } catch (Exception e) {}
			try { dao.delete(token2); } catch (Exception e) {}
			try { dao.delete(token3); } catch (Exception e) {}
		}
		
        System.out.println(" exception thrown?  expected false actual " + actuallyThrewException);
		Assert.assertFalse(actuallyThrewException, exceptionMsg);
	}
	
	@DataProvider(name="internalUserProvider")
	public Object[][] internalUserProvider() throws Exception
	{
		return new Object[][] {
			{ createAuthenticationToken(), INTERNAL_USER_CREATOR, true, "Token found for creating user.", false },
			{ createAuthenticationToken(), INTERNAL_USER_STRANGER, false, "Token not found for creating user.", false },
		};
	}
	
	@Test( dataProvider="internalUserProvider")
	public void findByUsernameReturnsOnlyUsernameTokens(AuthenticationToken token, String username, boolean shouldFindToken, String message, boolean shouldThrowException ) 
	{
		AuthenticationTokenDao dao = new AuthenticationTokenDao();
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		
		try 
		{
			dao.persist(token);

			Assert.assertNotNull(token.getId(), "AuthenticationToken got an id after persisting.");
			
			List<AuthenticationToken> tokens = dao.findAllByUsername(username);
			
			Assert.assertNotNull(tokens, "No internal tokens were found.");
			
			Assert.assertEquals(tokens.contains(token), shouldFindToken, "Persisted token was found.");
			
		} 
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Error persisting token " + token.getToken() + ": " + message;
            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
		}
		finally
		{
			try { dao.delete(token); } catch (Exception e) {}
		}
		
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
	}

	@Test
	public void findById() 
	{
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		AuthenticationToken user1 = null;
		
		try 
		{
			user1 = createAuthenticationToken();
			
			dao.persist(user1);

			Assert.assertNotNull(user1.getId(), "AuthenticationToken got an id after persisting.");
			
			AuthenticationToken savedToken = dao.findById(user1.getId());
			
			Assert.assertNotNull(savedToken, "AuthenticationTokens were found matching example.");
			
			Assert.assertTrue(savedToken.equals(user1), "Persisted token was found.");
			
		} 
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Error finding token by username.";
            e.printStackTrace();
		}
		finally
		{
			try { dao.delete(user1); } catch (Exception e) {}
		}
		
        System.out.println(" exception thrown?  expected false actual " + actuallyThrewException);
		Assert.assertFalse(actuallyThrewException, exceptionMsg);
	}
    @AfterClass
    public void tearDown(){
        HibernateUtil.flush();
        HibernateUtil.closeSession();
        HibernateUtil.getSessionFactory().close();

    }
}
