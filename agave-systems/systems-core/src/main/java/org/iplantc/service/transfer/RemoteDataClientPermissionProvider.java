package org.iplantc.service.transfer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.enumerations.PermissionType;

/**
 * Created with IntelliJ IDEA.
 * User: wcs
 * Date: 3/1/13
 * Time: 9:42 AM
 * To change this template use File | Settings | File Templates.
 */
public interface RemoteDataClientPermissionProvider
{
    public List<RemoteFilePermission> getAllPermissionsWithUserFirst(String path, String username) throws RemoteDataException, IOException, FileNotFoundException;

    public List<RemoteFilePermission> getAllPermissions(String path) throws RemoteDataException, IOException, FileNotFoundException;

    public PermissionType getPermissionForUser(String username, String path) throws RemoteDataException, IOException, FileNotFoundException;

    public boolean hasReadPermission(String path, String username) throws RemoteDataException, IOException, FileNotFoundException;

    public boolean hasWritePermission(String path, String username) throws RemoteDataException, IOException, FileNotFoundException;

    public boolean hasExecutePermission(String path, String username) throws RemoteDataException, IOException, FileNotFoundException;

    public void setPermissionForUser(String username, String path, PermissionType type, boolean recursive) throws RemoteDataException, IOException, FileNotFoundException;

    public void setOwnerPermission(String username, String path, boolean recursive) throws RemoteDataException, IOException, FileNotFoundException;

    public void setReadPermission(String username, String path, boolean recursive) throws RemoteDataException, IOException, FileNotFoundException;

    public void removeReadPermission(String username, String path, boolean recursive) throws RemoteDataException, IOException, FileNotFoundException;

    public void setWritePermission(String username, String path, boolean recursive) throws RemoteDataException, IOException, FileNotFoundException;

    public void removeWritePermission(String username, String path, boolean recursive) throws RemoteDataException, IOException, FileNotFoundException;

    public void setExecutePermission(String username, String path, boolean recursive) throws RemoteDataException, IOException, FileNotFoundException;

    public void removeExecutePermission(String username, String path, boolean recursive) throws RemoteDataException, IOException, FileNotFoundException;

    public void clearPermissions(String username, String path, boolean recursive) throws RemoteDataException, IOException, FileNotFoundException;

    public String getPermissions(String path) throws RemoteDataException, IOException, FileNotFoundException;

    public boolean isPermissionMirroringRequired();
}
