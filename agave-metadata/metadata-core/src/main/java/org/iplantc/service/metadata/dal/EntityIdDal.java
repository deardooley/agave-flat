package org.iplantc.service.metadata.dal;

import java.math.BigInteger;

import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.util.ServiceUtils;

/**
 * Created with IntelliJ IDEA.
 * User: wcs
 * Date: 9/5/13
 * Time: 12:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class EntityIdDal {

    /**
     * Returns all metadata permissions for the given oid.
     *
     * @param uuid
     * @return
     * @throws org.iplantc.service.metadata.exceptions.MetadataException
     */
    public static long getByUuid(AgaveUUID uuid) throws MetadataException
    {
        // ObjectType should be an enum value and prevent injection attacks.
        String tableName = uuid.getResourceType().toString().toLowerCase() + "s";
        if (!ServiceUtils.isValid(uuid.toString()))
            throw new MetadataException("Object id cannot be null");
        if (!ServiceUtils.isValid(tableName))
            throw new MetadataException("Table cannot be null");

        try {
            //HibernateUtil.beginTransaction();
            Session session = HibernateUtil.getSession();
            Query query = session.createSQLQuery("select id from " + tableName + " where uuid = :uuid");
            query.setString("uuid", uuid.toString());
            BigInteger id = (BigInteger)query.uniqueResult();

            if (id == null)
                throw new MetadataException("No such uuid present");

            return id.longValue();

        } catch(Exception e) {
            throw new MetadataException(e);
        //} finally {
            //try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
        }
    }
}

