/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/generic/CmsVfsDriver.java,v $
 * Date   : $Date: 2003/09/16 09:42:20 $
 * Version: $Revision: 1.128 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2003 Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.db.generic;

import org.opencms.db.CmsDbUtil;
import org.opencms.db.CmsDriverManager;
import org.opencms.db.I_CmsDriver;
import org.opencms.db.I_CmsVfsDriver;
import org.opencms.main.CmsEvent;
import org.opencms.main.I_CmsEventListener;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsAdjacencyTree;
import org.opencms.util.CmsUUID;

import com.opencms.boot.I_CmsLogChannels;
import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.file.CmsFile;
import com.opencms.file.CmsFolder;
import com.opencms.file.CmsObject;
import com.opencms.file.CmsProject;
import com.opencms.file.CmsPropertydefinition;
import com.opencms.file.CmsResource;
import com.opencms.file.CmsResourceTypeFolder;
import com.opencms.file.CmsUser;
import com.opencms.file.I_CmsResourceType;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import source.org.apache.java.util.Configurations;

/**
 * Generic (ANSI-SQL) database server implementation of the VFS driver methods.<p>
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.128 $ $Date: 2003/09/16 09:42:20 $
 * @since 5.1
 */
public class CmsVfsDriver extends Object implements I_CmsDriver, I_CmsVfsDriver {

    protected CmsDriverManager m_driverManager;
    protected org.opencms.db.generic.CmsSqlManager m_sqlManager;

    /**
     * Returns the amount of properties for a propertydefinition.
     *
     * @param metadef The propertydefinition to test.
     *
     * @return the amount of properties for a propertydefinition.
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    protected int internalCountProperties(CmsPropertydefinition metadef) throws CmsException {
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        int returnValue;
        try {
            // create statement
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_PROPERTIES_READALL_COUNT");
            stmt.setInt(1, metadef.getId());
            res = stmt.executeQuery();

            if (res.next()) {
                returnValue = res.getInt(1);
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + metadef.getName(), CmsException.C_UNKNOWN_EXCEPTION);
            }
        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            // close all db-resources
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return returnValue;
    }

    protected int internalCountSiblings(int projectId, CmsUUID resourceId) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet res = null;
        int count = 0;

        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_COUNTLINKS");
            stmt.setString(1, resourceId.toString());
            res = stmt.executeQuery();

            if (res.next())
                count = res.getInt(1);

        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }

        return count;
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#createCmsResourceFromResultSet(ResultSet, int)
     */
    public CmsFile createCmsFileFromResultSet(ResultSet res, int projectId) throws SQLException, CmsException {
        CmsUUID structureId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_STRUCTURE_ID")));
        CmsUUID resourceId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_RESOURCE_ID")));
        CmsUUID parentId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_PARENT_ID")));
        int resourceType = res.getInt(m_sqlManager.get("C_RESOURCES_RESOURCE_TYPE"));
        String resourceName = res.getString(m_sqlManager.get("C_RESOURCES_RESOURCE_NAME"));
        int resourceFlags = res.getInt(m_sqlManager.get("C_RESOURCES_RESOURCE_FLAGS"));
        CmsUUID fileId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_FILE_ID")));
        int resourceState = res.getInt(m_sqlManager.get("C_RESOURCES_STATE"));
        int structureState = res.getInt(m_sqlManager.get("C_RESOURCES_STRUCTURE_STATE"));
        int loaderId = res.getInt(m_sqlManager.get("C_RESOURCES_LOADER_ID"));
        long dateCreated = CmsDbUtil.getTimestamp(res, m_sqlManager.get("C_RESOURCES_DATE_CREATED")).getTime();
        long dateLastModified = CmsDbUtil.getTimestamp(res, m_sqlManager.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
        int resourceSize = res.getInt(m_sqlManager.get("C_RESOURCES_SIZE"));
        CmsUUID userCreated = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_USER_CREATED")));
        CmsUUID userLastModified = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_USER_LASTMODIFIED")));
        byte[] content = m_sqlManager.getBytes(res, m_sqlManager.get("C_RESOURCES_FILE_CONTENT"));
        int linkCount = res.getInt(m_sqlManager.get("C_RESOURCES_LINK_COUNT"));

        int newState = (structureState > resourceState) ? structureState : resourceState;

        return new CmsFile(structureId, resourceId, parentId, fileId, resourceName, resourceType, resourceFlags, projectId, newState, loaderId, dateCreated, userCreated, dateLastModified, userLastModified, resourceSize, linkCount, content);
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#createCmsFileFromResultSet(java.sql.ResultSet, int, boolean)
     */
    public CmsFile createCmsFileFromResultSet(ResultSet res, int projectId, boolean hasFileContentInResultSet) throws SQLException, CmsException {
        byte[] content = null;
        int resProjectId = I_CmsConstants.C_UNKNOWN_ID;

        CmsUUID structureId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_STRUCTURE_ID")));
        CmsUUID resourceId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_RESOURCE_ID")));
        CmsUUID parentId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_PARENT_ID")));
        String resourceName = res.getString(m_sqlManager.get("C_RESOURCES_RESOURCE_NAME"));
        int resourceType = res.getInt(m_sqlManager.get("C_RESOURCES_RESOURCE_TYPE"));
        int resourceFlags = res.getInt(m_sqlManager.get("C_RESOURCES_RESOURCE_FLAGS"));
        CmsUUID fileId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_FILE_ID")));
        int resourceState = res.getInt(m_sqlManager.get("C_RESOURCES_STATE"));
        int structureState = res.getInt(m_sqlManager.get("C_RESOURCES_STRUCTURE_STATE"));
        int loaderId = res.getInt(m_sqlManager.get("C_RESOURCES_LOADER_ID"));
        long dateCreated = CmsDbUtil.getTimestamp(res, m_sqlManager.get("C_RESOURCES_DATE_CREATED")).getTime();
        long dateLastModified = CmsDbUtil.getTimestamp(res, m_sqlManager.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
        int resourceSize = res.getInt(m_sqlManager.get("C_RESOURCES_SIZE"));
        CmsUUID userCreated = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_USER_CREATED")));
        CmsUUID userLastModified = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_USER_LASTMODIFIED")));
        int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
        int linkCount = res.getInt(m_sqlManager.get("C_RESOURCES_LINK_COUNT"));

        if (hasFileContentInResultSet) {
            content = m_sqlManager.getBytes(res, m_sqlManager.get("C_RESOURCES_FILE_CONTENT"));
        } else {
            content = new byte[0];
        }

        resProjectId = lockedInProject;

        int newState = (structureState > resourceState) ? structureState : resourceState;

        return new CmsFile(structureId, resourceId, parentId, fileId, resourceName, resourceType, resourceFlags, resProjectId, newState, loaderId, dateCreated, userCreated, dateLastModified, userLastModified, resourceSize, linkCount, content);
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#createCmsFolderFromResultSet(java.sql.ResultSet, int, boolean)
     */
    public CmsFolder createCmsFolderFromResultSet(ResultSet res, int projectId, boolean hasProjectIdInResultSet) throws SQLException {
        int resProjectId = I_CmsConstants.C_UNKNOWN_ID;

        CmsUUID structureId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_STRUCTURE_ID")));
        CmsUUID resourceId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_RESOURCE_ID")));
        CmsUUID parentId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_PARENT_ID")));
        String resourceName = res.getString(m_sqlManager.get("C_RESOURCES_RESOURCE_NAME"));
        int resourceType = res.getInt(m_sqlManager.get("C_RESOURCES_RESOURCE_TYPE"));
        int resourceFlags = res.getInt(m_sqlManager.get("C_RESOURCES_RESOURCE_FLAGS"));
        CmsUUID fileId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_FILE_ID")));
        int resourceState = res.getInt(m_sqlManager.get("C_RESOURCES_STATE"));
        int structureState = res.getInt(m_sqlManager.get("C_RESOURCES_STRUCTURE_STATE"));
        CmsUUID lockedBy = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_LOCKED_BY")));
        long dateCreated = CmsDbUtil.getTimestamp(res, m_sqlManager.get("C_RESOURCES_DATE_CREATED")).getTime();
        long dateLastModified = CmsDbUtil.getTimestamp(res, m_sqlManager.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
        CmsUUID userCreated = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_USER_CREATED")));
        CmsUUID userLastModified = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_USER_LASTMODIFIED")));
        int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
        int linkCount = res.getInt(m_sqlManager.get("C_RESOURCES_LINK_COUNT"));

        // TODO VFS links: refactor all upper methods to support the VFS link type param 

        if (!lockedBy.equals(CmsUUID.getNullUUID())) {
            // resource is locked
            resProjectId = lockedInProject;
        } else {
            // resource is not locked
            resProjectId = lockedInProject = projectId;
        }

        int newState = (structureState > resourceState) ? structureState : resourceState;

        return new CmsFolder(structureId, resourceId, parentId, fileId, resourceName, resourceType, resourceFlags, resProjectId, newState, dateCreated, userCreated, dateLastModified, userLastModified, linkCount);
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#createCmsResourceFromResultSet(java.sql.ResultSet, int)
     */
    public CmsResource createCmsResourceFromResultSet(ResultSet res, int projectId) throws SQLException, CmsException {

        CmsUUID structureId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_STRUCTURE_ID")));
        CmsUUID resourceId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_RESOURCE_ID")));
        CmsUUID parentId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_PARENT_ID")));
        String resourceName = res.getString(m_sqlManager.get("C_RESOURCES_RESOURCE_NAME"));
        int resourceType = res.getInt(m_sqlManager.get("C_RESOURCES_RESOURCE_TYPE"));
        int resourceFlags = res.getInt(m_sqlManager.get("C_RESOURCES_RESOURCE_FLAGS"));
        int resourceProjectId = res.getInt(m_sqlManager.get("C_RESOURCES_PROJECT_ID"));
        CmsUUID fileId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_FILE_ID")));
        int resourceState = res.getInt(m_sqlManager.get("C_RESOURCES_STATE"));
        int structureState = res.getInt(m_sqlManager.get("C_RESOURCES_STRUCTURE_STATE"));
        int loaderId = res.getInt(m_sqlManager.get("C_RESOURCES_LOADER_ID"));
        long dateCreated = CmsDbUtil.getTimestamp(res, m_sqlManager.get("C_RESOURCES_DATE_CREATED")).getTime();
        long dateLastModified = CmsDbUtil.getTimestamp(res, m_sqlManager.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
        int resourceSize = res.getInt(m_sqlManager.get("C_RESOURCES_SIZE"));
        CmsUUID userCreated = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_USER_CREATED")));
        CmsUUID userLastModified = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_USER_LASTMODIFIED")));
        int linkCount = res.getInt(m_sqlManager.get("C_RESOURCES_LINK_COUNT"));

        int newState = (structureState > resourceState) ? structureState : resourceState;

        CmsResource newResource = new CmsResource(structureId, resourceId, parentId, fileId, resourceName, resourceType, resourceFlags, resourceProjectId, newState, loaderId, dateCreated, userCreated, dateLastModified, userLastModified, resourceSize, linkCount);

        return newResource;
    }

    /**
     * Creates a new file from an given CmsFile object and a new filename.<p>
     * 
     * <b>The CmsFile object requires it's full resource name to be set!</b>
     * Pls. refer to {@link com.opencms.file.CmsResource#getFullResourceName()}.
     *
     * @param project The project in which the resource will be used.
     * @param file The file to be written to the Cms.
     * @param userId The Id of the user who changed the resourse.
     * @param parentId The parentId of the resource.
     * @param filename The complete new name of the file (including pathinformation).
     * @return file The created file.
     * @throws CmsException if something goes wrong
     * @throws RuntimeException if the file has not it's full resource name set
     * @see com.opencms.file.CmsResource#getFullResourceName()
     * @see com.opencms.file.CmsResource#setFullResourceName(String)
     */
    public CmsFile createFile(CmsProject project, CmsFile file, CmsUUID userId, CmsUUID parentId, String filename) throws CmsException {
        int newState = 0;
        CmsUUID modifiedByUserId = null, createdByUserId = null, newStructureId = null;
        long dateModified = 0, dateCreated = 0;
        Connection conn = null;
        PreparedStatement stmt = null;

        // validate if the file has it's full resource name set
        if (!file.hasFullResourceName()) {
            throw new RuntimeException("Full resource name not set for CmsFile " + file.getName());
        }

        // validate the resource name
        if (filename.length() > I_CmsConstants.C_MAX_LENGTH_RESOURCE_NAME) {
            throw new CmsException("The resource name '" + filename + "' is too long! (max. allowed length must be <= " + I_CmsConstants.C_MAX_LENGTH_RESOURCE_NAME + " chars.!)", CmsException.C_BAD_NAME);
        }

        // force some attribs when creating or publishing a file 
        if (project.getId() == I_CmsConstants.C_PROJECT_ONLINE_ID) {
            newState = I_CmsConstants.C_STATE_UNCHANGED;

            dateCreated = file.getDateCreated();
            createdByUserId = file.getUserCreated();

            dateModified = file.getDateLastModified();
            modifiedByUserId = file.getUserLastModified();
        } else {
            newState = I_CmsConstants.C_STATE_NEW;

            dateCreated = System.currentTimeMillis();
            createdByUserId = userId;

            dateModified = dateCreated;
            modifiedByUserId = createdByUserId;
        }

        // check if the resource already exists
        CmsResource res = null;
        newStructureId = file.getStructureId();
        try {
            res = readFileHeader(project.getId(), parentId, filename, true);
            res.setFullResourceName(file.getRootPath());
            if (res.getState() == I_CmsConstants.C_STATE_DELETED) {
                // if an existing resource is deleted, it will be finally removed now
                // but we have to reuse its id in order to avoid orphanes in the online project
                newStructureId = res.getStructureId();
                newState = I_CmsConstants.C_STATE_CHANGED;

                // remove the existing file and it's properties
                List modifiedResources = getAllVfsLinks(project, res); 
                deleteAllProperties(project.getId(), res);
                removeFile(project, res);

                OpenCms.fireCmsEvent(new CmsEvent(new CmsObject(), I_CmsEventListener.EVENT_RESOURCES_MODIFIED, Collections.singletonMap("resources", modifiedResources)));
                OpenCms.fireCmsEvent(new CmsEvent(new CmsObject(), I_CmsEventListener.EVENT_PROPERTIES_MODIFIED, Collections.singletonMap("resource", res)));
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_FILE_EXISTS);
            }
        } catch (CmsException e) {
            if (e.getType() == CmsException.C_FILE_EXISTS) {
                // we have a collision which has to be handled in the app.
                throw e;
            }
        }

        try {
            conn = m_sqlManager.getConnection(project);

            // write the structure
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_STRUCTURE_WRITE");
            stmt.setString(1, newStructureId.toString());
            stmt.setString(2, parentId.toString());
            stmt.setString(3, file.getResourceId().toString());
            stmt.setString(4, filename);
            stmt.setInt(5, newState);
            stmt.executeUpdate();

            m_sqlManager.closeAll(null, stmt, null);

            if (!existsResourceId(project.getId(), file.getResourceId())) {

                // write the resource
                stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_WRITE");
                stmt.setString(1, file.getResourceId().toString());
                stmt.setInt(2, file.getType());
                stmt.setInt(3, file.getFlags());
                stmt.setString(4, file.getFileId().toString());
                stmt.setInt(5, file.getLoaderId());
                stmt.setTimestamp(6, new Timestamp(dateCreated));
                stmt.setString(7, createdByUserId.toString());
                stmt.setTimestamp(8, new Timestamp(dateModified));
                stmt.setString(9, modifiedByUserId.toString());
                stmt.setInt(10, newState);
                stmt.setInt(11, file.getLength());
                stmt.setString(12, CmsUUID.getNullUUID().toString());
                stmt.setInt(13, project.getId());
                stmt.setInt(14, 1);
                stmt.executeUpdate();

                // write the content
                createFileContent(file.getFileId(), file.getContents(), 0, project.getId(), false);
            } else {

                // update the link Count
                stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_LINK_COUNT");
                stmt.setInt(1, this.internalCountSiblings(project.getId(), file.getResourceId()));
                stmt.setString(2, file.getResourceId().toString());
                stmt.executeUpdate();

                m_sqlManager.closeAll(null, stmt, null);

                // update the resource flags
                stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_FLAGS");
                stmt.setInt(1, file.getFlags());
                stmt.setString(2, file.getResourceId().toString());
                stmt.executeUpdate();

                //updateResourcestate(file, CmsDriverManager.C_UPDATE_RESOURCE);        
                writeFileContent(file.getFileId(), file.getContents(), project.getId(), false);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }

        return readFile(project.getId(), false, newStructureId);
    }

    /**
     * Creates a new file with the given content and resourcetype.<p>
     *
     * @param user The user who wants to create the file.
     * @param project The project in which the resource will be used.
     * @param filename The complete name of the new file (including pathinformation).
     * @param flags The flags of this resource.
     * @param parentFolder The parent folder of the resource.
     * @param contents The contents of the new file.
     * @param resourceType The resourceType of the new file.
     * @return file The created file.
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile createFile(CmsUser user, CmsProject project, String filename, int flags, CmsFolder parentFolder, byte[] contents, I_CmsResourceType resourceType) throws CmsException {

        CmsFile newFile = new CmsFile(new CmsUUID(), new CmsUUID(), parentFolder.getStructureId(), new CmsUUID(), filename, resourceType.getResourceType(), flags, project.getId(), com.opencms.core.I_CmsConstants.C_STATE_NEW, resourceType.getLoaderId(), 0, user.getId(), 0, user.getId(), contents.length, 1, contents);

        newFile.setFullResourceName(parentFolder.getRootPath() + newFile.getName());
        return createFile(project, newFile, user.getId(), parentFolder.getStructureId(), filename);
    }

    /**
     * Creates the content entry for a file
     *
     * @param fileId The ID of the new file
     * @param fileContent The content of the new file
     * @param versionId For the content of a backup file you need to insert the versionId of the backup
     * @throws CmsException if an error occurs
     */
    public void createFileContent(CmsUUID fileId, byte[] fileContent, int versionId, int projectId, boolean writeBackup) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            if (writeBackup) {
                conn = m_sqlManager.getConnectionForBackup();
                stmt = m_sqlManager.getPreparedStatement(conn, "C_FILES_WRITE_BACKUP");
            } else {
                conn = m_sqlManager.getConnection(projectId);
                stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_FILES_WRITE");
            }

            stmt.setString(1, fileId.toString());

            if (fileContent.length < 2000) {
                stmt.setBytes(2, fileContent);
            } else {
                stmt.setBinaryStream(2, new ByteArrayInputStream(fileContent), fileContent.length);
            }

            if (writeBackup) {
                stmt.setInt(3, versionId);
                stmt.setString(4, new CmsUUID().toString());
            }

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Creates a new folder from an existing folder object.
     *
     * @param user The user who wants to create the folder.
     * @param project The project in which the resource will be used.
     * @param folder The folder to be written to the Cms.
     * @param parentId The parentId of the resource.
     *
     * @param foldername The complete path of the new name of this folder.
     *
     * @return The created folder.
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public CmsFolder createFolder(CmsProject project, CmsFolder folder, CmsUUID parentId) throws CmsException {
        int state = 0;

        // validate the resource name
        if (folder.getName().length() > I_CmsConstants.C_MAX_LENGTH_RESOURCE_NAME) {
            throw new CmsException("The resource name '" + folder.getName() + "' is too long! (max. allowed length must be <= " + I_CmsConstants.C_MAX_LENGTH_RESOURCE_NAME + " chars.!)", CmsException.C_BAD_NAME);
        }

        // adjust the last-modified/creation dates
        long dateModified = folder.getDateLastModified();
        if (dateModified == 0) {
            dateModified = System.currentTimeMillis();
        }

        long dateCreated = folder.getDateCreated();
        if (dateCreated == 0) {
            dateCreated = System.currentTimeMillis();
        }

        if (project.getId() == I_CmsConstants.C_PROJECT_ONLINE_ID) {
            state = folder.getState();
        } else {
            state = I_CmsConstants.C_STATE_NEW;
        }

        // prove if a deleted folder with the same name inside the same folder exists
        try {
            CmsFolder oldFolder = readFolder(project.getId(), parentId, folder.getName());
            if (oldFolder.getState() == I_CmsConstants.C_STATE_DELETED) {
                throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_FILE_EXISTS);
            } else {
                if (oldFolder != null) {
                    throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_FILE_EXISTS);
                }
            }
        } catch (CmsException e) {
            if (e.getType() == CmsException.C_FILE_EXISTS) {
                throw e;
            }
        }

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = m_sqlManager.getConnection(project);

            // write the structure
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_STRUCTURE_WRITE");
            stmt.setString(1, folder.getStructureId().toString());
            stmt.setString(2, parentId.toString());
            stmt.setString(3, folder.getResourceId().toString());
            stmt.setString(4, folder.getName());
            stmt.setInt(5, state);
            stmt.executeUpdate();

            m_sqlManager.closeAll(null, stmt, null);

            if (!existsResourceId(project.getId(), folder.getResourceId())) {

                // write the resource
                stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_WRITE");
                stmt.setString(1, folder.getResourceId().toString());
                stmt.setInt(2, folder.getType());
                stmt.setInt(3, folder.getFlags());
                stmt.setString(4, CmsUUID.getNullUUID().toString());
                stmt.setInt(5, folder.getLoaderId());
                stmt.setTimestamp(6, new Timestamp(dateCreated));
                stmt.setString(7, folder.getUserCreated().toString());
                stmt.setTimestamp(8, new Timestamp(dateModified));
                stmt.setString(9, folder.getUserLastModified().toString());
                stmt.setInt(10, state);
                stmt.setInt(11, folder.getLength());
                stmt.setString(12, CmsUUID.getNullUUID().toString());
                stmt.setInt(13, project.getId());
                stmt.setInt(14, 1);
                stmt.executeUpdate();

            } else {

                // update the link Count
                stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_LINK_COUNT");
                stmt.setInt(1, this.internalCountSiblings(project.getId(), folder.getResourceId()));
                stmt.setString(2, folder.getResourceId().toString());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }

        // if this is the rootfolder or if the parentfolder is the rootfolder
        // try to create the projectresource
        String parentFolderName = "/";
        if (!folder.getName().equals(I_CmsConstants.C_ROOT)) {
            parentFolderName = folder.getName();
            if (parentFolderName.endsWith("/"))
                parentFolderName = parentFolderName.substring(0, parentFolderName.length() - 1);
            parentFolderName = parentFolderName.substring(0, parentFolderName.lastIndexOf("/") + 1);
        }

        if (parentId.isNullUUID() || parentFolderName.equals(I_CmsConstants.C_ROOT)) {
            try {
                String rootFolder = null;
                try {
                    rootFolder = m_driverManager.getProjectDriver().readProjectResource(project.getId(), I_CmsConstants.C_ROOT);
                } catch (CmsException exc) {
                    // NOOP
                }

                if (rootFolder == null) {
                    m_driverManager.getProjectDriver().createProjectResource(project.getId(), folder.getName());
                }

                //createProjectResource(project.getId(), foldername);
            } catch (CmsException e) {
                if (e.getType() != CmsException.C_FILE_EXISTS) {
                    throw e;
                }
            }
        }

        return readFolder(project.getId(), folder.getStructureId());
    }

    /**
      * Creates a new folder
      *
      * @param user The user who wants to create the folder.
      * @param project The project in which the resource will be used.
      * @param parentId The parentId of the folder.
      * @param fileId The fileId of the folder.
      * @param folderName The complete path to the folder in which the new folder will be created.
      * @param flags The flags of this resource.
      * @param dateLastModified the overwrite modification timestamp
      * @param userLastModified the overwrite modification user
      * @param dateCreated the overwrite creation timestamp  
      * @param userCreated the overwrite creation user         
      *
      * @return The created folder.
      * @throws CmsException Throws CmsException if operation was not succesful.
      */
    public CmsFolder createFolder(CmsProject project, CmsUUID parentId, CmsUUID fileId, String folderName, int flags, long dateLastModified, CmsUUID userLastModified, long dateCreated, CmsUUID userCreated) throws CmsException {

        CmsFolder newFolder = new CmsFolder(new CmsUUID(), new CmsUUID(), parentId, CmsUUID.getNullUUID(), folderName, CmsResourceTypeFolder.C_RESOURCE_TYPE_ID, flags, project.getId(), com.opencms.core.I_CmsConstants.C_STATE_NEW, dateCreated, userCreated, dateLastModified, userLastModified, 1);

        return createFolder(project, newFolder, parentId);

    }

    /**
     * Creates the propertydefinitions for the resource type.<BR/>
     *
     * Only the admin can do this.
     *
     * @param name The name of the propertydefinitions to overwrite.
     * @param resourcetype The resource-type for the propertydefinitions.
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public CmsPropertydefinition createPropertydefinition(String name, int projectId, int resourcetype) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            for (int i = 0; i < 3; i++) {
                // create the offline property definition
                if (i == 0) {
                    conn = m_sqlManager.getConnection();
                    stmt = m_sqlManager.getPreparedStatement(conn, Integer.MAX_VALUE, "C_PROPERTYDEF_CREATE");
                    stmt.setInt(1, m_sqlManager.nextId(m_sqlManager.get("C_TABLE_PROPERTYDEF")));
                }
                // create the online property definition
                else if (i == 1) {
                    conn = m_sqlManager.getConnection(I_CmsConstants.C_PROJECT_ONLINE_ID);
                    stmt = m_sqlManager.getPreparedStatement(conn, I_CmsConstants.C_PROJECT_ONLINE_ID, "C_PROPERTYDEF_CREATE");
                    stmt.setInt(1, m_sqlManager.nextId(m_sqlManager.get("C_TABLE_PROPERTYDEF_ONLINE")));
                }
                // create the backup property definition
                else {
                    conn = m_sqlManager.getConnectionForBackup();
                    stmt = m_sqlManager.getPreparedStatement(conn, "C_PROPERTYDEF_CREATE_BACKUP");
                    stmt.setInt(1, m_sqlManager.nextId(m_sqlManager.get("C_TABLE_PROPERTYDEF_BACKUP")));
                }
                stmt.setString(2, name);
                stmt.setInt(3, resourcetype);
                stmt.executeUpdate();
                m_sqlManager.closeAll(conn, stmt, null);
            }
        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }

        return readPropertydefinition(name, projectId, resourcetype);
    }

    /**
     * Creates a new link from an given CmsResource object and a new filename.<p>
     * 
     * <b>The CmsResource object requires it's full resource name to be set!</b>
     * Pls. refer to {@link com.opencms.file.CmsResource#getFullResourceName()}.
     * 
     * @param project the project where to create the link
     * @param resource the link prototype
     * @param userId the id of the user creating the link
     * @param parentId the id of the folder where the link is created
     * @param filename the name of the link
     * @return a valid link resource
     * @throws CmsException if something goes wrong
     * @throws RuntimeException if the resource has not it's full resource name set
     */
    public CmsResource createVfsLink(CmsProject project, CmsResource resource, CmsUUID userId, CmsUUID parentId, String filename) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        int newState = 0;

        // validate if the file has it's full resource name set
        if (!resource.hasFullResourceName()) {
            throw new RuntimeException("Full resource name not set for CmsResource " + resource.getName());
        }

        // validate the resource name
        if (filename.length() > I_CmsConstants.C_MAX_LENGTH_RESOURCE_NAME) {
            throw new CmsException("The resource name '" + filename + "' is too long! (max. allowed length must be <= " + I_CmsConstants.C_MAX_LENGTH_RESOURCE_NAME + " chars.!)", CmsException.C_BAD_NAME);
        }

        // force some attribs when creating or publishing a file 
        if (project.getId() == I_CmsConstants.C_PROJECT_ONLINE_ID) {
            newState = I_CmsConstants.C_STATE_UNCHANGED;
        } else {
            newState = I_CmsConstants.C_STATE_NEW;
        }

        // check if the resource already exists
        CmsResource res = null;
        CmsUUID newStructureId = resource.getStructureId();
        try {
            res = readFileHeader(project.getId(), parentId, filename, true);
            res.setFullResourceName(resource.getRootPath());
            if (res.getState() == I_CmsConstants.C_STATE_DELETED) {
                // if an existing resource is deleted, it will be finally removed now
                // but we have to reuse its id in order to avoid orphanes in the online project
                newStructureId = res.getStructureId();
                newState = I_CmsConstants.C_STATE_CHANGED;

                // remove the existing file and it's properties
                List modifiedResources = getAllVfsLinks(project, res);
                deleteAllProperties(project.getId(), res);
                removeFile(project, res);

                OpenCms.fireCmsEvent(new CmsEvent(new CmsObject(), I_CmsEventListener.EVENT_RESOURCES_MODIFIED, Collections.singletonMap("resources", modifiedResources)));
                OpenCms.fireCmsEvent(new CmsEvent(new CmsObject(), I_CmsEventListener.EVENT_PROPERTIES_MODIFIED, Collections.singletonMap("resource", res)));
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_FILE_EXISTS);
            }
        } catch (CmsException e) {
            if (e.getType() == CmsException.C_FILE_EXISTS) {
                // we have a collision which has to be handled in the app.
                throw e;
            }
        }

        // check if the resource already exists
        if (!existsResourceId(project.getId(), resource.getResourceId())) {
            throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_NOT_FOUND);
        }

        // write a new structure referring to the resource
        try {
            conn = m_sqlManager.getConnection(project);

            // write the structure
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_STRUCTURE_WRITE");
            stmt.setString(1, newStructureId.toString());
            stmt.setString(2, parentId.toString());
            stmt.setString(3, resource.getResourceId().toString());
            stmt.setString(4, filename);
            stmt.setInt(5, newState);
            stmt.executeUpdate();

            m_sqlManager.closeAll(null, stmt, null);

            // update the link Count
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_LINK_COUNT");
            stmt.setInt(1, this.internalCountSiblings(project.getId(), resource.getResourceId()));
            stmt.setString(2, resource.getResourceId().toString());
            stmt.executeUpdate();

            m_sqlManager.closeAll(null, stmt, null);

            // update the resource flags
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_FLAGS");
            stmt.setInt(1, resource.getFlags());
            stmt.setString(2, resource.getResourceId().toString());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }

        return readFileHeader(project.getId(), newStructureId, false);
    }

    /**
     * Deletes all properties for a file or folder.
     *
     * @param projectId the id of the project
     * @param resource the resource
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public void deleteAllProperties(int projectId, CmsResource resource) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        String resourceName = resource.getRootPath();

        // add folder separator to folder name if it is not present
        if (resource.isFolder() && !resourceName.endsWith(I_CmsConstants.C_FOLDER_SEPARATOR)) {
            resourceName += I_CmsConstants.C_FOLDER_SEPARATOR;
        }

        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_PROPERTIES_DELETEALL");
            stmt.setString(1, resource.getResourceId().toString());
            stmt.setString(2, resourceName);
            stmt.executeUpdate();
        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#deleteFile(com.opencms.file.CmsProject, com.opencms.file.CmsResource)
     */
    public void deleteFile(CmsProject currentProject, CmsResource resource) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = m_sqlManager.getConnection(currentProject);

            // delete the structure record
            stmt = m_sqlManager.getPreparedStatement(conn, currentProject, "C_RESOURCES_DELETE_STRUCTURE");
            stmt.setInt(1, I_CmsConstants.C_STATE_DELETED);
            stmt.setString(2, resource.getStructureId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Deletes the folder.<p>
     *
     * Only empty folders can be deleted yet.
     *
     * @param currentProject The project in which the resource will be used.
     * @param orgFolder The folder that will be deleted.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public void deleteFolder(CmsProject currentProject, CmsFolder orgFolder) throws CmsException {
        // the current implementation only deletes empty folders
        // check if the folder has any files in it
        List files = getSubResources(currentProject, orgFolder, false);
        files = internalFilterUndeletedResources(files);
        if (files.size() == 0) {
            // check if the folder has any folders in it
            List folders = getSubResources(currentProject, orgFolder, true);
            folders = internalFilterUndeletedResources(folders);
            if (folders.size() == 0) {
                //this folder is empty, delete it
                Connection conn = null;
                PreparedStatement stmt = null;
                try {
                    conn = m_sqlManager.getConnection(currentProject);
                    stmt = m_sqlManager.getPreparedStatement(conn, currentProject, "C_RESOURCES_REMOVE");
                    // mark the folder as deleted
                    stmt.setInt(1, com.opencms.core.I_CmsConstants.C_STATE_DELETED);
                    stmt.setString(2, CmsUUID.getNullUUID().toString());
                    stmt.setString(3, orgFolder.getResourceId().toString());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
                } finally {
                    m_sqlManager.closeAll(conn, stmt, null);
                }
            } else {
                String errorResNames = "";
                Iterator i = folders.iterator();
                while (i.hasNext()) {
                    CmsResource errorRes = (CmsResource) i.next();
                    errorResNames += "[" + errorRes.getName() + "]";
                }
                throw new CmsException("[" + this.getClass().getName() + "] " + orgFolder.getName() + errorResNames, CmsException.C_NOT_EMPTY);
            }
        } else {
            String errorResNames = "";
            Iterator i = files.iterator();
            while (i.hasNext()) {
                CmsResource errorRes = (CmsResource) i.next();
                errorResNames += "[" + errorRes.getName() + "]";
            }
            throw new CmsException("[" + this.getClass().getName() + "] " + orgFolder.getName() + errorResNames, CmsException.C_NOT_EMPTY);
        }
    }

    /**
     * Deletes a property for a file or folder.
     *
     * @param meta The property-name of which the property has to be read.
     * @param projectId the id of the project
     * @param resource The resource.
     * @param resourceType The Type of the resource.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public void deleteProperty(String meta, int projectId, CmsResource resource, int resourceType) throws CmsException {
        CmsPropertydefinition propdef = readPropertydefinition(meta, 0, resourceType);
        String resourceName = resource.getRootPath();

        // add folder separator to folder name if it is not present
        if (resourceType == CmsResourceTypeFolder.C_RESOURCE_TYPE_ID && !resourceName.endsWith(I_CmsConstants.C_FOLDER_SEPARATOR)) {
            resourceName += I_CmsConstants.C_FOLDER_SEPARATOR;
        }

        if (propdef == null) {
            // there is no propdefinition with the overgiven name for the resource
            throw new CmsException("[" + this.getClass().getName() + ".deleteProperty] " + meta, CmsException.C_NOT_FOUND);
        } else {
            // delete the metainfo in the db
            Connection conn = null;
            PreparedStatement stmt = null;

            try {
                // create statement
                conn = m_sqlManager.getConnection(projectId);
                stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_PROPERTIES_DELETE");
                stmt.setInt(1, propdef.getId());
                stmt.setString(2, resource.getResourceId().toString());
                stmt.setString(3, resourceName);
                stmt.executeUpdate();
            } catch (SQLException exc) {
                throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
            } finally {
                m_sqlManager.closeAll(conn, stmt, null);
            }
        }
    }

    /**
     * Delete the propertydefinitions for the resource type.<BR/>
     *
     * Only the admin can do this.
     *
     * @param metadef The propertydefinitions to be deleted.
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public void deletePropertydefinition(CmsPropertydefinition metadef) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            if (internalCountProperties(metadef) != 0) {
                throw new CmsException("[" + this.getClass().getName() + "] " + metadef.getName(), CmsException.C_UNKNOWN_EXCEPTION);
            }
            for (int i = 0; i < 3; i++) {
                // delete the propertydef from offline db
                if (i == 0) {
                    conn = m_sqlManager.getConnection();
                    stmt = m_sqlManager.getPreparedStatement(conn, "C_PROPERTYDEF_DELETE");
                }
                // delete the propertydef from online db
                else if (i == 1) {
                    conn = m_sqlManager.getConnection(I_CmsConstants.C_PROJECT_ONLINE_ID);
                    stmt = m_sqlManager.getPreparedStatement(conn, I_CmsConstants.C_PROJECT_ONLINE_ID, "C_PROPERTYDEF_DELETE");
                }
                // delete the propertydef from backup db
                else {
                    conn = m_sqlManager.getConnectionForBackup();
                    stmt = m_sqlManager.getPreparedStatement(conn, "C_PROPERTYDEF_DELETE_BACKUP");
                }
                stmt.setInt(1, metadef.getId());
                stmt.executeUpdate();
                m_sqlManager.closeAll(conn, stmt, null);
            }
        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#destroy()
     */
    public void destroy() throws Throwable {
        finalize();

        if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_INIT)) {
            OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, "[" + this.getClass().getName() + "] destroyed!");
        }
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#existsContentId(int, com.opencms.flex.util.CmsUUID)
     */
    public boolean existsContentId(int projectId, CmsUUID contentId) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet res = null;
        boolean result = false;
        int count = 0;

        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_SELECT_CONTENT_ID");
            stmt.setString(1, contentId.toString());
            res = stmt.executeQuery();

            if (res.next()) {
                count = res.getInt(1);
                result = (count == 1);
            } else {
                result = false;
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return result;
    }

    /**
     * Tests if a resource with the given resourceId does already exist in the Database.<p>
     * 
     * @param projectId the project id
     * @param resourceId the resource id to test for
     * @return true if a resource with the given id was found, false otherweise
     * @throws CmsException if something goes wrong
     */
    public boolean existsResourceId(int projectId, CmsUUID resourceId) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet res = null;
        boolean exists = false;

        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_READ_RESOURCE_STATE");
            stmt.setString(1, resourceId.toString());
            res = stmt.executeQuery();

            exists = res.next();

        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }

        return exists;
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#existsStructureId(int, com.opencms.flex.util.CmsUUID)
     */
    public boolean existsStructureId(int projectId, CmsUUID structureId) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet res = null;
        boolean result = false;
        int count = 0;

        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_SELECT_STRUCTURE_ID");
            stmt.setString(1, structureId.toString());
            res = stmt.executeQuery();

            if (res.next()) {
                count = res.getInt(1);
                result = (count == 1);
            } else {
                result = false;
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return result;
    }

    /**
     * @see java.lang.Object#finalize()
     */
    protected void finalize() throws Throwable {
        if (m_sqlManager != null) {
            m_sqlManager.finalize();
        }

        m_sqlManager = null;
        m_driverManager = null;
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#getAllVfsLinks(CmsProject, CmsResource)
     */
    public List getAllVfsLinks(CmsProject currentProject, CmsResource resource) throws CmsException {
        PreparedStatement stmt = null;
        Connection conn = null;
        ResultSet res = null;
        CmsResource currentResource = null;
        List vfsLinks = (List) new ArrayList();

        try {
            conn = m_sqlManager.getConnection(currentProject);
            stmt = m_sqlManager.getPreparedStatement(conn, currentProject, "C_SELECT_NONDELETED_VFS_LINKS");
            stmt.setString(1, resource.getResourceId().toString());
            // stmt.setInt(2, com.opencms.core.I_CmsConstants.C_STATE_DELETED);
            res = stmt.executeQuery();

            while (res.next()) {
                currentResource = createCmsFileFromResultSet(res, currentProject.getId(), false);
                vfsLinks.add(currentResource);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return vfsLinks;
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#getAllVfsSoftLinks(CmsProject, CmsResource)
     */
    // TODO: neccessary / should be renamed
    public List getAllVfsSoftLinks(CmsProject currentProject, CmsResource resource) throws CmsException {
        PreparedStatement stmt = null;
        Connection conn = null;
        ResultSet res = null;
        CmsResource currentResource = null;
        List vfsLinks = (List) new ArrayList();

        try {
            conn = m_sqlManager.getConnection(currentProject);
            stmt = m_sqlManager.getPreparedStatement(conn, currentProject, "C_SELECT_NONDELETED_SIBLINGS");
            stmt.setString(1, resource.getResourceId().toString());
            stmt.setString(2, resource.getStructureId().toString());
            //stmt.setInt(3, I_CmsConstants.C_VFS_LINK_TYPE_SLAVE);
            //stmt.setInt(3, com.opencms.core.I_CmsConstants.C_STATE_DELETED);
            res = stmt.executeQuery();

            while (res.next()) {
                currentResource = createCmsFileFromResultSet(res, currentProject.getId(), false);
                vfsLinks.add(currentResource);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return vfsLinks;
    }

    /**
     * Returns a Vector with all resource-names that have set the given property to the given value.
     *
     * @param projectId the id of the project to test.
     * @param propertyDefinition the name of the propertydefinition to check.
     * @param propertyValue the value of the property for the resource.
     * @return Vector with all names of resources.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getFilesWithProperty(int projectId, String propertyDefinition, String propertyValue) throws CmsException {
        Vector names = new Vector();
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_GET_FILES_WITH_PROPERTY");
            stmt.setInt(1, projectId);
            stmt.setString(2, propertyValue);
            stmt.setString(3, propertyDefinition);
            res = stmt.executeQuery();

            // store the result into the vector
            while (res.next()) {
                String result = res.getString(1);
                names.addElement(result);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception exc) {
            throw m_sqlManager.getCmsException(this, "getFilesWithProperty(int, String, String)", CmsException.C_UNKNOWN_EXCEPTION, exc, false);
        } finally {
            // close all db-resources
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return names;
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#getFolderTree(com.opencms.file.CmsProject, com.opencms.file.CmsResource)
     */
    public List getFolderTree(CmsProject currentProject, CmsResource parentResource) throws CmsException {
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        CmsAdjacencyTree adjacencyTree = new CmsAdjacencyTree();
        CmsUUID parentId = null;
        CmsUUID structureId = null;

        /*
         * possible other SQL queries to select a tree view:
         * SELECT PARENT.RESOURCE_NAME, CHILD.RESOURCE_NAME FROM CMS_OFFLINE_STRUCTURE PARENT, CMS_OFFLINE_STRUCTURE CHILD WHERE PARENT.STRUCTURE_ID=CHILD.PARENT_ID;
         * SELECT PARENT.RESOURCE_NAME, CHILD.RESOURCE_NAME FROM CMS_OFFLINE_STRUCTURE PARENT LEFT JOIN CMS_OFFLINE_STRUCTURE CHILD ON PARENT.STRUCTURE_ID=CHILD.PARENT_ID; 
         */

        try {
            conn = m_sqlManager.getConnection(currentProject);
            stmt = m_sqlManager.getPreparedStatement(conn, currentProject, "C_RESOURCES_GET_FOLDERTREE");
            stmt.setInt(1, I_CmsConstants.C_STATE_CHANGED);
            stmt.setInt(2, I_CmsConstants.C_STATE_NEW);
            stmt.setInt(3, I_CmsConstants.C_STATE_UNCHANGED);
            res = stmt.executeQuery();

            while (res.next()) {
                parentId = new CmsUUID(res.getString(1));
                structureId = new CmsUUID(res.getString(2));
                adjacencyTree.add(parentId, structureId);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception ex) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, ex, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        List dfsList = adjacencyTree.toList(parentResource.getStructureId());
        return dfsList;
    }

    /**
     * Reads all resources (including the folders) residing in a folder<BR>
     *
     * @param projectId the id of the project
     * @param offlineResource the parent resource id of the offline resoure.
     *
     * @return A Vecor of resources.
     *
     * @throws CmsException if operation was not succesful
     */
    public Vector getResourcesInFolder(int projectId, CmsFolder offlineResource) throws CmsException {
        Vector resources = new Vector();
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        CmsResource currentResource = null;
        CmsFolder currentFolder = null;

        // first get the folderst
        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_GET_FOLDERS_IN_FOLDER");
            stmt.setString(1, offlineResource.getStructureId().toString());
            //stmt.setInt(2, projectId);
            res = stmt.executeQuery();

            while (res.next()) {
                currentFolder = createCmsFolderFromResultSet(res, projectId, true);
                resources.addElement(currentFolder);
            }

        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception ex) {
            throw new CmsException("[" + this.getClass().getName() + "]", ex);
        } finally {
            m_sqlManager.closeAll(null, stmt, res);
        }

        // then get the resources
        try {
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_GET_RESOURCES_IN_FOLDER");
            stmt.setString(1, offlineResource.getStructureId().toString());
            //stmt.setInt(2, projectId);
            res = stmt.executeQuery();

            while (res.next()) {
                currentResource = createCmsResourceFromResultSet(res, projectId);
                resources.addElement(currentResource);
            }

        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception ex) {
            throw new CmsException("[" + this.getClass().getName() + "]", ex);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return resources;
    }

    /**
    * Gets all resources with a modification date within a given time frame.<p>
    * 
    * @param projectId the current project
    * @param starttime the begin of the time range
    * @param endtime the end of the time range
    * @return List with all resources
    *
    * @throws CmsException if operation was not succesful
    * @see org.opencms.db.I_CmsVfsDriver#getResourcesInTimeRange(int currentProject, long startime, long endtime)
    */
    public List getResourcesInTimeRange(int projectId, long starttime, long endtime) throws CmsException {
        List result = new ArrayList();

        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_GET_RESOURCE_IN_TIMERANGE");
            stmt.setTimestamp(1, new Timestamp(starttime));
            stmt.setTimestamp(2, new Timestamp(endtime));
            res = stmt.executeQuery();

            while (res.next()) {
                CmsResource resource = createCmsResourceFromResultSet(res, projectId);
                result.add(resource);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception exc) {
            throw new CmsException("getResourcesWithProperty" + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return result;
    }

    /**
     * Returns a Vector with all resources of the given type
     * that have set the given property. For the start it is
     * only used by the static export so it reads the online project only.
     *
     * @param projectId the id of the project to test.
     * @param propertyDefName the name of the propertydefinition to check.
     *
     * @return Vector with all resources.
     *
     * @throws CmsException if operation was not succesful.
     */
    public Vector getResourcesWithProperty(int projectId, String propertyDefName) throws CmsException {
        Vector resources = new Vector();
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_GET_RESOURCE_WITH_PROPERTYDEF");
            stmt.setString(1, propertyDefName);
            res = stmt.executeQuery();

            while (res.next()) {
                CmsResource resource = createCmsResourceFromResultSet(res, projectId);
                resources.addElement(resource);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return resources;
    }

    /**
     * Returns a Vector with all resources of the given type
     * that have set the given property to the given value.
     *
     * @param projectId the id of the project to test.
     * @param propertyDefinition the name of the propertydefinition to check.
     * @param propertyValue the value of the property for the resource.
     * @param resourceType the value of the resourcetype.
     *
     * @return Vector with all resources.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getResourcesWithProperty(int projectId, String propertyDefinition, String propertyValue, int resourceType) throws CmsException {
        Vector resources = new Vector();
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_GET_RESOURCE_WITH_PROPERTY");
            stmt.setInt(1, projectId);
            stmt.setString(2, propertyValue);
            stmt.setString(3, propertyDefinition);
            stmt.setInt(4, resourceType);
            res = stmt.executeQuery();
            String lastResourcename = "";

            while (res.next()) {
                CmsResource resource = createCmsResourceFromResultSet(res, projectId);
                if (!resource.getName().equalsIgnoreCase(lastResourcename)) {
                    resources.addElement(resource);
                }
                lastResourcename = resource.getName();
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception exc) {
            throw new CmsException("getResourcesWithProperty" + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return resources;
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#getSubResources(com.opencms.file.CmsProject, com.opencms.file.CmsFolder, boolean)
     */
    public List getSubResources(CmsProject currentProject, CmsFolder parentFolder, boolean getSubFolders) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet res = null;
        CmsResource currentResource = null;
        List subResources = (List) new ArrayList();
        String query = null;
        String resourceTypeClause = null;
        String orderClause = " ORDER BY CMS_T_STRUCTURE.RESOURCE_NAME";

        if (getSubFolders) {
            resourceTypeClause = " AND CMS_T_RESOURCES.RESOURCE_TYPE=0";
        } else {
            resourceTypeClause = " AND CMS_T_RESOURCES.RESOURCE_TYPE<>0";
        }

        try {
            conn = m_sqlManager.getConnection(currentProject);
            query = m_sqlManager.get(currentProject, "C_RESOURCES_GET_SUBRESOURCES") + CmsSqlManager.replaceTableKey(currentProject.getId(), resourceTypeClause + orderClause);
            stmt = m_sqlManager.getPreparedStatementForSql(conn, query);
            stmt.setString(1, parentFolder.getStructureId().toString());
            res = stmt.executeQuery();

            while (res.next()) {
                if (getSubFolders) {
                    currentResource = createCmsFolderFromResultSet(res, currentProject.getId(), false);
                } else {
                    currentResource = createCmsFileFromResultSet(res, currentProject.getId(), false);
                }
                subResources.add(currentResource);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return subResources;
    }

    /**
     * Gets all resources that are marked as undeleted.
     * 
     * @param resources Vector of resources
     * @return Returns all resources that are markes as deleted
     */
    protected List internalFilterUndeletedResources(List resources) {
        List undeletedResources = (List) new ArrayList();
        
        for (int i = 0; i < resources.size(); i++) {
            CmsResource res = (CmsResource) resources.get(i);
            if (res.getState() != I_CmsConstants.C_STATE_DELETED) {
                undeletedResources.add(res);
            }
        }
        
        return undeletedResources;
    }

    /**
     * Creates a new resource from an given CmsResource object.
     *
     * @param project The project in which the resource will be used.
     * @param newResource The resource to be written to the Cms.
     * @param filecontent The filecontent if the resource is a file
     * @param userId The ID of the current user.
     * @param parentId The parentId of the resource.
     * @param isFolder true to create a new folder
     *
     * @return resource The created resource.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsResource importResource(CmsProject project, CmsUUID parentId, CmsResource newResource, byte[] filecontent, CmsUUID userId, boolean isFolder) throws CmsException {

        Connection conn = null;
        PreparedStatement stmt = null;

        if (newResource.getName().length() > I_CmsConstants.C_MAX_LENGTH_RESOURCE_NAME) {
            throw new CmsException("The resource name '" + newResource.getName() + "' is too long! (max. allowed length must be <= " + I_CmsConstants.C_MAX_LENGTH_RESOURCE_NAME + " chars.!)", CmsException.C_BAD_NAME);
        }

        int state = 0;
        CmsUUID modifiedByUserId = newResource.getUserLastModified();
        //long dateModified = newResource.isTouched() ? newResource.getDateLastModified() : System.currentTimeMillis();

        if (project.getId() == I_CmsConstants.C_PROJECT_ONLINE_ID) {
            state = newResource.getState();
            modifiedByUserId = newResource.getUserLastModified();
            //dateModified = newResource.getDateLastModified();
        } else {
            state = I_CmsConstants.C_STATE_NEW;
        }

        try {
            CmsResource curResource = readResource(project, parentId, newResource.getName());
            if (curResource.getState() == I_CmsConstants.C_STATE_DELETED) {
                throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_RESOURCE_DELETED);
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_FILE_EXISTS);
            }

        } catch (CmsException e) {
            // if the resource is marked as deleted remove it!
            if (e.getType() == CmsException.C_RESOURCE_DELETED) {
                if (isFolder) {
                    removeFolder(project, (CmsFolder) newResource);
                    state = I_CmsConstants.C_STATE_CHANGED;
                } else {
                    //removeFile(project, parentId, newResource.getResourceName());
                }
                //throw new CmsException("["+this.getClass().getName()+"] ",CmsException.C_FILE_EXISTS);
            }
            if (e.getType() == CmsException.C_FILE_EXISTS) {
                throw e;
            }
        }

        // check if we can use some existing UUID's             
        CmsUUID newFileId = CmsUUID.getNullUUID();
        CmsUUID resourceId = new CmsUUID();
        CmsUUID structureId = new CmsUUID();
        if (newResource.getStructureId() != CmsUUID.getNullUUID()) {
            structureId = newResource.getStructureId();
        }
        if (newResource.getResourceId() != CmsUUID.getNullUUID()) {
            resourceId = newResource.getResourceId();
        }
        if (newResource.getFileId() != CmsUUID.getNullUUID()) {
            newFileId = newResource.getFileId();
        }

        // now write the resource
        try {
            conn = m_sqlManager.getConnection(project);

            // write the structure                                  
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_STRUCTURE_WRITE");
            stmt.setString(1, structureId.toString());
            stmt.setString(2, parentId.toString());
            stmt.setString(3, resourceId.toString());
            stmt.setString(4, newResource.getName());
            stmt.setInt(5, state);
            stmt.executeUpdate();

            if (!existsResourceId(project.getId(), newResource.getResourceId())) {

                // write the resource
                stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_WRITE");
                stmt.setString(1, resourceId.toString());
                stmt.setInt(2, newResource.getType());
                stmt.setInt(3, newResource.getFlags());
                stmt.setString(4, newFileId.toString());
                stmt.setInt(5, newResource.getLoaderId());
                stmt.setTimestamp(6, new Timestamp(newResource.getDateCreated()));
                stmt.setString(7, newResource.getUserCreated().toString());
                stmt.setTimestamp(8, new Timestamp(newResource.getDateLastModified()));
                stmt.setString(9, modifiedByUserId.toString());
                stmt.setInt(10, state);
                stmt.setInt(11, newResource.getLength());
                stmt.setString(12, CmsUUID.getNullUUID().toString());
                stmt.setInt(13, project.getId());
                stmt.setInt(14, 1);
                stmt.executeUpdate();
                m_sqlManager.closeAll(null, stmt, null);

                //write the content
                if (!isFolder) {
                    try {
                        createFileContent(newFileId, filecontent, 0, project.getId(), false);
                    } catch (CmsException se) {
                        if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                            OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + "] " + se.getMessage());
                        }
                    }
                }

            } else {
                // update the link Count
                stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_LINK_COUNT");
                stmt.setInt(1, this.internalCountSiblings(project.getId(), newResource.getResourceId()));
                stmt.setString(2, newResource.getResourceId().toString());
                stmt.executeUpdate();

                m_sqlManager.closeAll(null, stmt, null);

                // update the resource flags
                stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_FLAGS");
                stmt.setInt(1, newResource.getFlags());
                stmt.setString(2, newResource.getResourceId().toString());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }

        return readResource(project, parentId, newResource.getName());
    }

    /**
     * @see org.opencms.db.I_CmsDriver#init(source.org.apache.java.util.Configurations, java.util.List, org.opencms.db.CmsDriverManager)
     */
    public void init(Configurations config, List successiveDrivers, CmsDriverManager driverManager) {
        String offlinePoolUrl = null;
        String onlinePoolUrl = null;
        String backupPoolUrl = null;
        boolean hasDistinctPoolUrls = false;

        if ((offlinePoolUrl = config.getString("db.vfs.pool")) == null) {
            hasDistinctPoolUrls = true;
            offlinePoolUrl = config.getString("db.vfs.offline.pool");
            onlinePoolUrl = config.getString("db.vfs.online.pool");
            backupPoolUrl = config.getString("db.vfs.backup.pool");
        } else {
            hasDistinctPoolUrls = false;
            onlinePoolUrl = backupPoolUrl = offlinePoolUrl;
        }

        m_sqlManager = this.initQueries();
        m_sqlManager.setOfflinePoolUrl(offlinePoolUrl);
        m_sqlManager.setOnlinePoolUrl(onlinePoolUrl);
        m_sqlManager.setBackupPoolUrl(backupPoolUrl);

        m_driverManager = driverManager;

        if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_INIT)) {
            if (hasDistinctPoolUrls) {
                OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Assign. offline pool : " + offlinePoolUrl);
                OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Assign. online pool  : " + onlinePoolUrl);
                OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Assign. backup pool  : " + backupPoolUrl);
            } else {
                OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Assign. pool         : " + offlinePoolUrl);
            }
        }

        if (successiveDrivers != null && !successiveDrivers.isEmpty()) {
            if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_INIT)) {
                OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, this.getClass().toString() + " does not support successive drivers.");
            }
        }
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#initQueries(java.lang.String)
     */
    public org.opencms.db.generic.CmsSqlManager initQueries() {
        return new org.opencms.db.generic.CmsSqlManager();
    }

    public List readAllFileHeaders(CmsProject currentProject) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet res = null;
        CmsResource currentResource = null;
        List allResources = (List) new ArrayList();

        try {
            conn = m_sqlManager.getConnection(currentProject);
            stmt = m_sqlManager.getPreparedStatement(conn, currentProject, "C_RESOURCES_READ_ALL");
            res = stmt.executeQuery();

            while (res.next()) {
                currentResource = createCmsResourceFromResultSet(res, currentProject.getId());
                allResources.add(currentResource);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return allResources;
    }

    /**
     * Reads all propertydefinitions for the given resource type.
     *
     * @param projectId the id of the project
     * @param resourcetype The resource type to read the propertydefinitions for.
     *
     * @return propertydefinitions A Vector with propertydefefinitions for the resource type.
     * The Vector is maybe empty.
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public Vector readAllPropertydefinitions(int projectId, I_CmsResourceType resourcetype) throws CmsException {
        Vector metadefs = new Vector();
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_PROPERTYDEF_READALL");
            // create statement
            stmt.setInt(1, resourcetype.getResourceType());
            res = stmt.executeQuery();

            while (res.next()) {
                metadefs.addElement(new CmsPropertydefinition(res.getInt(m_sqlManager.get("C_PROPERTYDEF_ID")), res.getString(m_sqlManager.get("C_PROPERTYDEF_NAME")), res.getInt(m_sqlManager.get("C_PROPERTYDEF_RESOURCE_TYPE"))));
            }
        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return (metadefs);
    }

    /**
     * Reads a file from the Cms.<BR/>
     *
     * @param projectId The Id of the project in which the resource will be used.
     * @param onlineProjectId The online projectId of the OpenCms.
     * @param filename The complete name of the new file (including pathinformation).
     *
     * @return file The read file.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile readFile(int projectId, boolean includeDeleted, CmsUUID structureId) throws CmsException {
        CmsFile file = null;
        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;
        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_FILES_READ");
            stmt.setString(1, structureId.toString());
            //stmt.setInt(2, projectId);
            res = stmt.executeQuery();
            if (res.next()) {
                file = createCmsFileFromResultSet(res, projectId);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
                // check if this resource is marked as deleted
                if (file.getState() == com.opencms.core.I_CmsConstants.C_STATE_DELETED && !includeDeleted) {
                    throw new CmsException("[" + this.getClass().getName() + ".readFile] " + file.getName(), CmsException.C_RESOURCE_DELETED);
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + ".readFile] " + structureId.toString(), CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (CmsException ex) {
            throw ex;
        } catch (Exception exc) {
            throw new CmsException("readFile " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return file;
    }

    /**
     * Reads a file header from the Cms.<BR/>
     * The reading excludes the filecontent.
     *
     * @param projectId The Id of the project
     * @param resourceId The Id of the resource.
     *
     * @return file The read file.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile readFileHeader(int projectId, CmsUUID resourceId, boolean includeDeleted) throws CmsException {

        CmsFile file = null;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_READBYID");
            // read file data from database
            stmt.setString(1, resourceId.toString());
            res = stmt.executeQuery();
            // create new file
            if (res.next()) {
                file = createCmsFileFromResultSet(res, projectId, false);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
                // check if this resource is marked as deleted
                if ((file.getState() == com.opencms.core.I_CmsConstants.C_STATE_DELETED) && !includeDeleted) {
                    throw new CmsException("[" + this.getClass().getName() + ".readFileHeader/2] " + file.getName(), CmsException.C_RESOURCE_DELETED);
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + ".readFileHeader/2] " + resourceId, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (CmsException ex) {
            throw ex;
        } catch (Exception exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return file;
    }

    /**
     * Reads a file header from the Cms.<BR/>
     * The reading excludes the filecontent.
     *
     * @param projectId The Id of the project in which the resource will be used.
     * @param filename The complete name of the new file (including pathinformation).
     *
     * @return file The read file.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile readFileHeader(int projectId, CmsUUID parentId, String filename, boolean includeDeleted) throws CmsException {
        CmsFile file = null;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_READ");

            stmt.setString(1, filename);
            stmt.setString(2, parentId.toString());
            //stmt.setInt(3, projectId);

            res = stmt.executeQuery();

            if (res.next()) {
                file = createCmsFileFromResultSet(res, projectId, false);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }

                // check if this resource is marked as deleted
                if ((file.getState() == com.opencms.core.I_CmsConstants.C_STATE_DELETED) && !includeDeleted) {
                    throw new CmsException("[" + this.getClass().getName() + ".readFileHeader/3] " + file.getName(), CmsException.C_RESOURCE_DELETED);
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + ".readFileHeader/3] " + filename, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (CmsException ex) {
            throw ex;
        } catch (Exception exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return file;
    }

    /**
     * Reads all files from the Cms, that are in one project.<BR/>
     *
     * @param project The project in which the files are.
     *
     * @return A Vecor of files.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public List readFiles(int projectId) throws CmsException {
        List resources = (List) new ArrayList();
        CmsResource currentResource;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        //String queryName = null;

        /*
        if (includeUnchanged && onlyProject) {
            queryName = "C_RESOURCES_READ_FILES_BY_PROJECT";
        } else if (includeUnchanged && !onlyProject) {
            queryName = "C_RESOURCES_READ_FILES";
        } else if (onlyProject) {
            queryName = "C_RESOURCES_READ_CHANGED_FILES_BY_PROJECT";
        } else {
            //queryName = "C_RESOURCES_READ_CHANGED_FILES";
            queryName = "C_RESOURCES_READ_CHANGED_FILEHEADERS";
        }
        */

        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_READ_CHANGED_FILEHEADERS");
            /*
            if (onlyProject) {
                stmt.setInt(1, projectId);
            }
            */
            res = stmt.executeQuery();

            while (res.next()) {
                //currentResource = createCmsFileFromResultSet(res, projectId, true);
                currentResource = createCmsResourceFromResultSet(res, projectId);
                resources.add(currentResource);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception ex) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, ex, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return resources;
    }

    /**
     * Reads all files from the Cms, that are of the given type.<BR/>
     *
     * @param projectId A project id for reading online or offline resources
     * @param resourcetype The type of the files.
     *
     * @return A Vector of files.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector readFilesByType(int projectId, int resourcetype) throws CmsException {
        Vector files = new Vector();
        CmsFile file;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_READ_FILESBYTYPE");
            // read file data from database
            stmt.setInt(1, resourcetype);
            res = stmt.executeQuery();
            // create new file
            while (res.next()) {
                file = createCmsFileFromResultSet(res, projectId, true);
                files.addElement(file);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception ex) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, ex, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return files;
    }

    /**
     * Reads a folder from the Cms.<BR/>
     *
     * @param project The project in which the resource will be used.
     * @param folderid The id of the folder to be read.
     *
     * @return The read folder.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public CmsFolder readFolder(int projectId, CmsUUID folderId) throws CmsException {
        CmsFolder folder = null;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_READBYID");
            stmt.setString(1, folderId.toString());
            res = stmt.executeQuery();

            if (res.next()) {
                folder = createCmsFolderFromResultSet(res, projectId, true);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + ".readFolder/1] " + folderId, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (CmsException exc) {
            throw exc;
        } catch (Exception exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return folder;
    }

    /**
     * Reads a folder from the Cms.<BR/>
     *
     * @param project The project in which the resource will be used.
     * @param foldername The name of the folder to be read.
     *
     * @return The read folder.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public CmsFolder readFolder(int projectId, CmsUUID parentId, String foldername) throws CmsException {

        CmsFolder folder = null;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_READ");

            stmt.setString(1, foldername);
            stmt.setString(2, parentId.toString());

            res = stmt.executeQuery();

            if (res.next()) {
                folder = createCmsFolderFromResultSet(res, projectId, true);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }

            } else {
                throw new CmsException("[" + this.getClass().getName() + ".readFolder/2] " + foldername, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (CmsException exc) {
            throw exc;
        } catch (Exception exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return folder;

    }

    /**
     * Reads all folders from the Cms, that are in one project.<BR/>
     *
     * @param project The project in which the folders are.
     *
     * @return A Vecor of folders.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public List readFolders(int projectId) throws CmsException {
        List folders = (List) new ArrayList();
        CmsFolder currentFolder;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        /*
        String query = null;
        String changedClause = null;
        String projectClause = null;
        String orderClause = " ORDER BY CMS_T_STRUCTURE.STRUCTURE_ID ASC";
        
        if (projectId != I_CmsConstants.C_PROJECT_ONLINE_ID && onlyProject) {
            projectClause = " AND CMS_T_RESOURCES.PROJECT_ID=" + projectId;
        } else {
            projectClause = "";
        }
        
        if (!includeUnchanged) {
        	// TODO: dangerous - move this to query.properties
            changedClause = " AND (CMS_T_STRUCTURE.STRUCTURE_STATE!=" + I_CmsConstants.C_STATE_UNCHANGED + " OR CMS_T_RESOURCES.RESOURCE_STATE!=" + I_CmsConstants.C_STATE_UNCHANGED + ")";
        } else {
            changedClause = "";
        }        
        */

        try {
            conn = m_sqlManager.getConnection(projectId);
            //query = m_sqlManager.get(projectId, "C_RESOURCES_READ_FOLDERS_BY_PROJECT") + CmsSqlManager.replaceTableKey(projectId, projectClause + changedClause + orderClause);
            //stmt = m_sqlManager.getPreparedStatementForSql(conn, query);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_READ_CHANGED_FOLDERS_BY_PROJECT");
            // stmt.setInt(1, I_CmsConstants.C_STATE_UNCHANGED);
            // stmt.setInt(2, I_CmsConstants.C_STATE_UNCHANGED);
            res = stmt.executeQuery();
            while (res.next()) {
                currentFolder = createCmsFolderFromResultSet(res, projectId, true);
                folders.add(currentFolder);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception ex) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, ex, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return folders;
    }

    /**
     * Returns a list of all properties of a file or folder.<p>
     *
     * @param resourceId the id of the resource
     * @param resource the resource to read the properties from
     * @param resourceType the type of the resource
     *
     * @return a Map of Strings representing the properties of the resource
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Map readProperties(int projectId, CmsResource resource, int resourceType) throws CmsException {
        HashMap returnValue = new HashMap();
        ResultSet result = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        String resourceName = resource.getRootPath();
        // hack: this never should happen, but it does.......
        if ((resource.isFolder()) && (!resourceName.endsWith("/"))) {
            resourceName += "/";
        }

        CmsUUID resourceId = resource.getResourceId();
        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_PROPERTIES_READALL");
            stmt.setString(1, resourceId.toString());
            stmt.setString(2, resourceName);
            stmt.setInt(3, resourceType);
            result = stmt.executeQuery();
            while (result.next()) {
                returnValue.put(result.getString(m_sqlManager.get("C_PROPERTYDEF_NAME")), result.getString(m_sqlManager.get("C_PROPERTY_VALUE")));
            }
        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, result);
        }
        return (returnValue);
    }

    /**
     * Returns a property of a file or folder.
     *
     * @param meta The property-name of which the property has to be read.
     * @param resourceId The id of the resource.
     * @param resourceType The Type of the resource.
     *
     * @return property The property as string or null if the property not exists.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public String readProperty(String meta, int projectId, CmsResource resource, int resourceType) throws CmsException {

        ResultSet result = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        String returnValue = null;

        String resourceName = resource.getRootPath();
        // hack: this never should happen, but it does.......
        if ((resource.isFolder()) && (!resourceName.endsWith("/"))) {
            resourceName += "/";
        }

        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_PROPERTIES_READ");

            String resourceId = resource.getResourceId().toString();
            stmt.setString(1, meta);
            stmt.setInt(2, resourceType);
            stmt.setString(3, resourceId);
            stmt.setString(4, resourceName);

            result = stmt.executeQuery();
            if (result.next()) {
                returnValue = result.getString(m_sqlManager.get("C_PROPERTY_VALUE"));
            }
        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, result);
        }

        return returnValue;
    }

    /**
     * Reads a propertydefinition for the given resource type.
     *
     * @param name The name of the propertydefinition to read.
     * @param projectId the id of the project
     * @param type The resource type for which the propertydefinition is valid.
     *
     * @return propertydefinition The propertydefinition that corresponds to the overgiven
     * arguments - or null if there is no valid propertydefinition.
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public CmsPropertydefinition readPropertydefinition(String name, int projectId, int type) throws CmsException {
        CmsPropertydefinition propDef = null;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(projectId);
            stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_PROPERTYDEF_READ");

            stmt.setString(1, name);
            stmt.setInt(2, type);
            res = stmt.executeQuery();

            // if resultset exists - return it
            if (res.next()) {
                propDef = new CmsPropertydefinition(res.getInt(m_sqlManager.get("C_PROPERTYDEF_ID")), res.getString(m_sqlManager.get("C_PROPERTYDEF_NAME")), res.getInt(m_sqlManager.get("C_PROPERTYDEF_RESOURCE_TYPE")));
            } else {
                res.close();
                res = null;
                // not found!
                throw new CmsException("[" + this.getClass().getName() + ".readPropertydefinition] " + name, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return propDef;
    }

    /**
     * Reads a resource from the Cms.<BR/>
     * A resource is either a file header or a folder.
     *
     * @param project The project in which the resource will be used.
     * @param filename The complete name of the new file (including pathinformation).
     *
     * @return The resource read.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsResource readResource(CmsProject project, CmsUUID parentId, String filename) throws CmsException {
        CmsResource file = null;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(project);
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_READ");

            stmt.setString(1, filename);
            stmt.setString(2, parentId.toString());
            res = stmt.executeQuery();

            if (res.next()) {
                file = createCmsResourceFromResultSet(res, project.getId());
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + ".readResource] " + filename, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception exc) {
            throw new CmsException("readResource " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return file;
    }

    /**
     * Reads all resource from the Cms, that are in one project.<BR/>
     * A resource is either a file header or a folder.
     *
     * @param project The project in which the resource will be used.
     *
     * @return A Vecor of resources.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector readResources(CmsProject project) throws CmsException {

        Vector resources = new Vector();
        CmsResource file;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = m_sqlManager.getConnection(project);
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_READBYPROJECT");
            // read resource data from database
            stmt.setInt(1, project.getId());
            res = stmt.executeQuery();
            // create new resource
            while (res.next()) {
                file = createCmsResourceFromResultSet(res, project.getId());
                resources.addElement(file);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception ex) {
            throw new CmsException("[" + this.getClass().getName() + "]", ex);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return resources;
    }

    /**
     * Reads all resources that contains the given string in the resourcename
     * and exists in the current project.<BR/>
     * A resource is either a file header or a folder.
     *
     * @param project The project in which the resource will be used.
     * @param resourcename A part of the resourcename
     *
     * @return A Vecor of resources.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector readResourcesLikeName(CmsProject project, String resourcename) throws CmsException {

        Vector resources = new Vector();
        CmsResource file;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        String usedStatement = "";
        if (project.getId() == I_CmsConstants.C_PROJECT_ONLINE_ID) {
            usedStatement = "_ONLINE";
        } else {
            usedStatement = "";
        }
        try {
            conn = m_sqlManager.getConnection(project);
            // read resource data from database
            //stmt = conn.prepareStatement(m_sqlManager.get("C_RESOURCES_READ_LIKENAME_1" + usedStatement) + resourcename + m_sqlManager.get("C_RESOURCES_READ_LIKENAME_2" + usedStatement));
            stmt = m_sqlManager.getPreparedStatementForSql(conn, m_sqlManager.get("C_RESOURCES_READ_LIKENAME_1" + usedStatement) + resourcename + m_sqlManager.get("C_RESOURCES_READ_LIKENAME_2" + usedStatement));
            stmt.setInt(1, project.getId());
            res = stmt.executeQuery();
            // create new resource
            while (res.next()) {
                file = createCmsResourceFromResultSet(res, project.getId());
                resources.addElement(file);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception ex) {
            throw new CmsException("[" + this.getClass().getName() + "]", ex);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return resources;
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#removeFile(com.opencms.file.CmsProject, com.opencms.file.CmsResource)
     */
    public void removeFile(CmsProject currentProject, CmsResource resource) throws CmsException {
        PreparedStatement stmt = null;
        Connection conn = null;
        int linkCount = 0;

        try {
            conn = m_sqlManager.getConnection(currentProject);

            // delete the structure recourd
            stmt = m_sqlManager.getPreparedStatement(conn, currentProject, "C_STRUCTURE_DELETE_BY_STRUCTUREID");
            stmt.setString(1, resource.getStructureId().toString());
            stmt.executeUpdate();

            m_sqlManager.closeAll(null, stmt, null);

            // count the references to the resource
            linkCount = internalCountSiblings(currentProject.getId(), resource.getResourceId());

            if (linkCount > 0) {

                // update the link Count
                stmt = m_sqlManager.getPreparedStatement(conn, currentProject, "C_RESOURCES_UPDATE_LINK_COUNT");
                stmt.setInt(1, this.internalCountSiblings(currentProject.getId(), resource.getResourceId()));
                stmt.setString(2, resource.getResourceId().toString());
                stmt.executeUpdate();

                m_sqlManager.closeAll(null, stmt, null);

                // update the resource flags
                stmt = m_sqlManager.getPreparedStatement(conn, currentProject, "C_RESOURCES_UPDATE_FLAGS");
                stmt.setInt(1, resource.getFlags());
                stmt.setString(2, resource.getResourceId().toString());
                stmt.executeUpdate();

            } else {

                // if not referenced any longer, also delete the resource and the content record
                stmt = m_sqlManager.getPreparedStatement(conn, currentProject, "C_RESOURCES_DELETE_BY_RESOURCEID");
                stmt.setString(1, resource.getResourceId().toString());
                stmt.executeUpdate();

                m_sqlManager.closeAll(null, stmt, null);

                // delete the content record
                stmt = m_sqlManager.getPreparedStatement(conn, currentProject, "C_FILE_CONTENT_DELETE");
                stmt.setString(1, resource.getFileId().toString());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Removes a folder and its subfolders physically in the database.<p>
     * The contents of the folders must have been already deleted
     *
     * @param currentProject the current project
     * @param folder the folder
     * @throws CmsException if something goes wrong
     */
    public void removeFolder(CmsProject currentProject, CmsFolder folder) throws CmsException {
        // the current implementation only deletes empty folders
        // check if the folder has any files in it
        List files = getSubResources(currentProject, folder, false);
        files = internalFilterUndeletedResources(files);
        if (files.size() == 0) {
            // check if the folder has any folders in it
            List folders = getSubResources(currentProject, folder, true);
            folders = internalFilterUndeletedResources(folders);
            if (folders.size() == 0) {
                //this folder is empty, delete it
                // Connection conn = null;
                // PreparedStatement stmt = null;
                // try {
                //     conn = m_sqlManager.getConnection(currentProject);
                //     stmt = m_sqlManager.getPreparedStatement(conn, currentProject, "C_RESOURCES_ID_DELETE");
                //    // delete the folder
                //    stmt.setString(1, folder.getId().toString());
                //    stmt.executeUpdate();
                // } catch (SQLException e) {
                //    throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
                //} finally {
                //    m_sqlManager.closeAll(conn, stmt, null);
                // }
                internalRemoveFolder(currentProject, folder);
            } else {
                String errorResNames = "";
                Iterator i = folders.iterator();
                while (i.hasNext()) {
                    CmsResource errorRes = (CmsResource) i.next();
                    errorResNames += "[" + errorRes.getName() + "]";
                }
                throw new CmsException("[" + this.getClass().getName() + "] " + folder.getName() + errorResNames, CmsException.C_NOT_EMPTY);
            }
        } else {
            String errorResNames = "";
            Iterator i = files.iterator();
            while (i.hasNext()) {
                CmsResource errorRes = (CmsResource) i.next();
                errorResNames += "[" + errorRes.getName() + "]";
            }
            throw new CmsException("[" + this.getClass().getName() + "] " + folder.getName() + errorResNames, CmsException.C_NOT_EMPTY);
        }
    }

    /**
     * Removes a resource physically in the database.<p>
     *
     * @param currentProject the current project
     * @param structureId the structure id of the folder
     * @throws CmsException if something goes wrong
     */
    protected void internalRemoveFolder(CmsProject currentProject, CmsFolder folder) throws CmsException {
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(currentProject);

            // delete the structure record            
            stmt = m_sqlManager.getPreparedStatement(conn, currentProject, "C_STRUCTURE_DELETE_BY_STRUCTUREID");
            stmt.setString(1, folder.getStructureId().toString());
            stmt.executeUpdate();

            m_sqlManager.closeAll(null, stmt, null);

            // delete the resource record
            stmt = m_sqlManager.getPreparedStatement(conn, currentProject, "C_RESOURCES_DELETE_BY_RESOURCEID");
            stmt.setString(1, folder.getResourceId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Removes the temporary files of the given resource
     *
     * @param file The file of which the remporary files should be deleted
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public void removeTemporaryFile(CmsResource file) throws CmsException {
        PreparedStatement stmt = null;
        PreparedStatement statementCont = null;
        PreparedStatement statementProp = null;
        Connection conn = null;
        ResultSet res = null;
        String fileId = null;
        String structureId = null;
        boolean hasBatch = false;

        //String tempFilename = file.getRootName() + file.getPath() + I_CmsConstants.C_TEMP_PREFIX + file.getName() + "%";
        String tempFilename = I_CmsConstants.C_TEMP_PREFIX + file.getName() + "%";

        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_RESOURCES_GETTEMPFILES");
            stmt.setString(1, tempFilename);
            stmt.setString(2, file.getParentStructureId().toString());
            res = stmt.executeQuery();

            statementProp = m_sqlManager.getPreparedStatement(conn, "C_PROPERTIES_DELETEALL");
            statementCont = m_sqlManager.getPreparedStatement(conn, "C_FILE_CONTENT_DELETE");

            while (res.next()) {
                hasBatch = true;

                fileId = res.getString("FILE_ID");
                structureId = res.getString("STRUCTURE_ID");

                // delete the properties
                statementProp.setString(1, structureId);
                statementProp.addBatch();

                // delete the file content
                statementCont.setString(1, fileId);
                statementCont.addBatch();
            }

            if (hasBatch) {
                statementProp.executeBatch();
                statementCont.executeBatch();

                m_sqlManager.closeAll(null, stmt, res);

                stmt = m_sqlManager.getPreparedStatement(conn, "C_RESOURCES_DELETETEMPFILES");
                stmt.setString(1, tempFilename);
                stmt.setString(2, file.getParentStructureId().toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
            m_sqlManager.closeAll(null, statementProp, null);
            m_sqlManager.closeAll(null, statementCont, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#replaceResource(com.opencms.file.CmsUser, com.opencms.file.CmsProject, com.opencms.file.CmsResource, java.util.Map, byte[])
     */
    public void replaceResource(CmsUser currentUser, CmsProject currentProject, CmsResource res, byte[] resContent, int newResType, int loaderId) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            // write the file content
            if (resContent != null) {
                writeFileContent(res.getFileId(), resContent, currentProject.getId(), false);
            }

            // update the resource record
            conn = m_sqlManager.getConnection(currentProject);
            stmt = m_sqlManager.getPreparedStatement(conn, currentProject, "C_RESOURCE_REPLACE");
            stmt.setInt(1, newResType);
            stmt.setInt(2, resContent.length);
            stmt.setInt(3, loaderId);
            stmt.setString(4, res.getResourceId().toString());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#resetProjectId(CmsProject, CmsResource)
     */
    public void resetProjectId(CmsProject currentProject, CmsResource currentResource) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = m_sqlManager.getConnection(currentProject);
            stmt = m_sqlManager.getPreparedStatement(conn, currentProject, "C_RESOURCES_UPDATE_PROJECT_ID");
            stmt.setInt(1, 0);
            stmt.setString(2, currentResource.getResourceId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#updateProjectId(com.opencms.file.CmsProject, com.opencms.file.CmsResource)
     */
    public void updateProjectId(CmsProject project, CmsResource resource) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = m_sqlManager.getConnection(project);
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_PROJECT_ID");
            stmt.setInt(1, project.getId());
            stmt.setString(2, resource.getResourceId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsVfsDriver#publishResource(com.opencms.file.CmsResource, com.opencms.file.CmsResource)
     */
    public void updateResource(CmsResource onlineResource, CmsResource offlineResource) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        int resourceSize = I_CmsConstants.C_UNKNOWN_ID;

        try {
            conn = m_sqlManager.getConnection(I_CmsConstants.C_PROJECT_ONLINE_ID);

            if (existsResourceId(I_CmsConstants.C_PROJECT_ONLINE_ID, offlineResource.getResourceId())) {

                // the resource record exists online already

                if (offlineResource.isFile()) {
                    // update the online file content
                    resourceSize = offlineResource.getLength();
                    writeFileContent(offlineResource.getFileId(), ((CmsFile) offlineResource).getContents(), I_CmsConstants.C_PROJECT_ONLINE_ID, false);
                }

                // update the online resource record
                stmt = m_sqlManager.getPreparedStatement(conn, I_CmsConstants.C_PROJECT_ONLINE_ID, "C_RESOURCES_UPDATE_RESOURCES");
                stmt.setInt(1, offlineResource.getType());
                stmt.setInt(2, offlineResource.getFlags());
                stmt.setInt(3, offlineResource.getLoaderId());
                stmt.setTimestamp(4, new Timestamp(offlineResource.getDateLastModified()));
                stmt.setString(5, offlineResource.getUserLastModified().toString());
                stmt.setInt(6, I_CmsConstants.C_STATE_UNCHANGED);
                stmt.setInt(7, resourceSize);
                stmt.setString(8, offlineResource.getFileId().toString());
                stmt.setString(9, CmsUUID.getNullUUID().toString());
                stmt.setInt(10, offlineResource.getProjectLastModified());
                stmt.setInt(11, this.internalCountSiblings(I_CmsConstants.C_PROJECT_ONLINE_ID, onlineResource.getResourceId()));
                stmt.setString(12, offlineResource.getResourceId().toString());
                stmt.executeUpdate();

                m_sqlManager.closeAll(null, stmt, null);

                // update the online structure record
                stmt = m_sqlManager.getPreparedStatement(conn, I_CmsConstants.C_PROJECT_ONLINE_ID, "C_RESOURCES_UPDATE_STRUCTURE");
                stmt.setString(1, offlineResource.getParentStructureId().toString());
                stmt.setString(2, offlineResource.getResourceId().toString());
                stmt.setString(3, offlineResource.getName());
                stmt.setInt(4, I_CmsConstants.C_STATE_UNCHANGED);
                stmt.setString(5, offlineResource.getStructureId().toString());
                stmt.executeUpdate();

            } else {

                // the resource record does NOT exist online yet

                if (offlineResource.isFile() && !existsContentId(I_CmsConstants.C_PROJECT_ONLINE_ID, offlineResource.getFileId())) {
                    // create the file content online
                    resourceSize = offlineResource.getLength();
                    createFileContent(offlineResource.getFileId(), ((CmsFile) offlineResource).getContents(), 0, I_CmsConstants.C_PROJECT_ONLINE_ID, false);
                }

                // create the resource record online
                stmt = m_sqlManager.getPreparedStatement(conn, I_CmsConstants.C_PROJECT_ONLINE_ID, "C_RESOURCES_WRITE");
                stmt.setString(1, offlineResource.getResourceId().toString());
                stmt.setInt(2, offlineResource.getType());
                stmt.setInt(3, offlineResource.getFlags());
                stmt.setString(4, offlineResource.getFileId().toString());
                stmt.setInt(5, offlineResource.getLoaderId());
                stmt.setTimestamp(6, new Timestamp(offlineResource.getDateCreated()));
                stmt.setString(7, offlineResource.getUserCreated().toString());
                stmt.setTimestamp(8, new Timestamp(offlineResource.getDateLastModified()));
                stmt.setString(9, offlineResource.getUserLastModified().toString());
                stmt.setInt(10, I_CmsConstants.C_STATE_UNCHANGED);
                stmt.setInt(11, resourceSize);
                stmt.setString(12, CmsUUID.getNullUUID().toString());
                stmt.setInt(13, offlineResource.getProjectLastModified());
                stmt.setInt(14, 1);
                stmt.executeUpdate();

                m_sqlManager.closeAll(null, stmt, null);

                // create the structure record online
                stmt = m_sqlManager.getPreparedStatement(conn, I_CmsConstants.C_PROJECT_ONLINE_ID, "C_STRUCTURE_WRITE");
                stmt.setString(1, offlineResource.getStructureId().toString());
                stmt.setString(2, offlineResource.getParentStructureId().toString());
                stmt.setString(3, offlineResource.getResourceId().toString());
                stmt.setString(4, offlineResource.getName());
                stmt.setInt(5, I_CmsConstants.C_STATE_UNCHANGED);
                stmt.executeUpdate();

            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Updates the state of a Resource.
     *
     * @param res com.opencms.file.CmsResource
     * @throws com.opencms.core.CmsException The exception description.
     */
    public void updateResourceState(CmsProject project, CmsResource resource, int changed) throws CmsException {
        PreparedStatement stmt = null;
        Connection conn = null;

        if (project.getId() == I_CmsConstants.C_PROJECT_ONLINE_ID) {
            return;
        }

        try {
            conn = m_sqlManager.getConnection(project);

            if (changed == CmsDriverManager.C_UPDATE_RESOURCE) {
                stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_RESOURCE_STATELASTMODIFIED");
                stmt.setInt(1, resource.getState());
                stmt.setTimestamp(2, new Timestamp(resource.getDateLastModified()));
                stmt.setString(3, resource.getUserLastModified().toString());
                stmt.setInt(4, project.getId());
                stmt.setString(5, resource.getResourceId().toString());
                stmt.executeUpdate();
                m_sqlManager.closeAll(null, stmt, null);
            }

            if (changed == CmsDriverManager.C_UPDATE_RESOURCE_STATE || changed == CmsDriverManager.C_UPDATE_ALL) {
                stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_RESOURCE_STATE");
                stmt.setInt(1, resource.getState());
                stmt.setInt(2, project.getId());
                stmt.setString(3, resource.getResourceId().toString());
                stmt.executeUpdate();
                m_sqlManager.closeAll(null, stmt, null);
            }

            if (changed == CmsDriverManager.C_UPDATE_STRUCTURE || changed == CmsDriverManager.C_UPDATE_STRUCTURE_STATE || changed == CmsDriverManager.C_UPDATE_ALL) {
                stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_STRUCTURE_STATE");
                stmt.setInt(1, resource.getState());
                stmt.setString(2, resource.getStructureId().toString());
                stmt.executeUpdate();
            }

        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            // close all db-resources
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
    * Writes the file content of an existing file
    *
    * @param fileId The ID of the file to update
    * @param fileContent The new content of the file
    * @param usedPool The name of the database pool to use
    * @param usedStatement Specifies which tables must be used: offline, online or backup
    */
    public void writeFileContent(CmsUUID fileId, byte[] fileContent, int projectId, boolean writeBackup) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            if (writeBackup) {
                conn = m_sqlManager.getConnectionForBackup();
                stmt = m_sqlManager.getPreparedStatement(conn, "C_FILES_UPDATE_BACKUP");
            } else {
                conn = m_sqlManager.getConnection(projectId);
                stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_FILES_UPDATE");
            }

            // update the file content in the FILES database.
            if (fileContent.length < 2000) {
                stmt.setBytes(1, fileContent);
            } else {
                stmt.setBinaryStream(1, new ByteArrayInputStream(fileContent), fileContent.length);
            }

            stmt.setString(2, fileId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
    * Writes the fileheader to the Database.
    *
    * @param project the project in which the resource will be used
    * @param file the new file
    * @param changed flag indicating if the file state must be set to changed. possible values are 
    * <ul>
    * <li>C_UPDATE_RESOURCE_STATE: sets the state in the resource record to C_STATE_CHANGED, the state in the structure record to the passed state value</li>
    * <li>C_UPDATE_STRUCTURE_STATE: sets the state in the structure record to C_STATE_CHANGES, the state in the resource record to the passed state value</li>
    * <li>C_NOTHING_CHANGED: sets both the state in the structure and the resource record to the passed state value</li>
    * <li>otherwise both the state in the resource and the structure record are set to C_STATE_CHANGED</li>
    * </ul>
    * @param userId the id of the user who has changed the resource
    *
    * @throws CmsException if operation was not succesful
    */
    public void writeFileHeader(CmsProject project, CmsFile file, int changed, CmsUUID userId) throws CmsException {
        // this task is split into two statements because Oracle doesnt support muti-table updates
        PreparedStatement stmt = null;
        Connection conn = null;
        long resourceDateModified = file.isTouched() ? file.getDateLastModified() : System.currentTimeMillis();

        // since we are only writing the file header, the content is unchanged
        // for this reason, the resource state is left unchanged
        int structureState = file.getState();
        int resourceState = file.getState();
        //if (structureState != com.opencms.core.I_CmsConstants.C_STATE_NEW && (changed > CmsDriverManager.C_NOTHING_CHANGED)) {
        if (changed == CmsDriverManager.C_UPDATE_RESOURCE_STATE)
            resourceState = com.opencms.core.I_CmsConstants.C_STATE_CHANGED;
        else if (changed == CmsDriverManager.C_UPDATE_STRUCTURE_STATE)
            structureState = com.opencms.core.I_CmsConstants.C_STATE_CHANGED;
        else if (changed != CmsDriverManager.C_NOTHING_CHANGED)
            resourceState = structureState = com.opencms.core.I_CmsConstants.C_STATE_CHANGED;
        //}

        try {
            conn = m_sqlManager.getConnection(project);
            //savepoint = conn.setSavepoint("before_update");

            // update the resource
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_RESOURCES");
            stmt.setInt(1, file.getType());
            stmt.setInt(2, file.getFlags());
            stmt.setInt(3, file.getLoaderId());
            stmt.setTimestamp(4, new Timestamp(resourceDateModified));
            stmt.setString(5, userId.toString());
            stmt.setInt(6, resourceState);
            stmt.setInt(7, file.getLength());
            stmt.setString(8, file.getFileId().toString());
            stmt.setString(9, CmsUUID.getNullUUID().toString());
            stmt.setInt(10, project.getId());
            stmt.setInt(11, internalCountSiblings(project.getId(), file.getResourceId()));
            stmt.setString(12, file.getResourceId().toString());
            stmt.executeUpdate();
            m_sqlManager.closeAll(null, stmt, null);

            // update the structure
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_STRUCTURE");
            stmt.setString(1, file.getParentStructureId().toString());
            stmt.setString(2, file.getResourceId().toString());
            stmt.setString(3, file.getName());
            stmt.setInt(4, structureState);
            stmt.setString(5, file.getStructureId().toString());
            stmt.executeUpdate();

            //m_sqlManager.commit(conn);
        } catch (SQLException e) {
            //m_sqlManager.rollback(conn, savepoint);
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            //m_sqlManager.releaseSavepoint(conn, savepoint);
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
    * Writes a folder to the Cms.<BR/>
    *
    * @param project The project in which the resource will be used.
    * @param folder The folder to be written.
    * @param changed Flag indicating if the file state must be set to changed.
    * @param userId The user who has changed the resource
    *
    * @throws CmsException Throws CmsException if operation was not succesful.
    */
    public void writeFolder(CmsProject project, CmsFolder folder, int changed, CmsUUID userId) throws CmsException {
        PreparedStatement stmt = null;
        Connection conn = null;
        //CmsUUID modifiedByUserId = userId;
        long resourceDateModified = folder.isTouched() ? folder.getDateLastModified() : System.currentTimeMillis();

        //Savepoint savepoint = null;
        int structureState = folder.getState();
        int resourceState = folder.getState();
        if (structureState != com.opencms.core.I_CmsConstants.C_STATE_NEW && (changed > CmsDriverManager.C_NOTHING_CHANGED)) {
            if (changed == CmsDriverManager.C_UPDATE_RESOURCE_STATE)
                resourceState = com.opencms.core.I_CmsConstants.C_STATE_CHANGED;
            else if (changed == CmsDriverManager.C_UPDATE_STRUCTURE_STATE)
                structureState = com.opencms.core.I_CmsConstants.C_STATE_CHANGED;
            else
                resourceState = structureState = com.opencms.core.I_CmsConstants.C_STATE_CHANGED;
        }

        try {
            conn = m_sqlManager.getConnection(project);
            //savepoint = conn.setSavepoint("before_update");

            // update the resource
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_RESOURCES");
            stmt.setInt(1, folder.getType());
            stmt.setInt(2, folder.getFlags());
            stmt.setInt(3, folder.getLoaderId());
            stmt.setTimestamp(4, new Timestamp(resourceDateModified));
            stmt.setString(5, userId.toString());
            stmt.setInt(6, structureState);
            stmt.setInt(7, 0);
            stmt.setString(8, CmsUUID.getNullUUID().toString());
            stmt.setString(9, CmsUUID.getNullUUID().toString());
            //stmt.setInt(10, folder.getProjectId());
            stmt.setInt(10, project.getId());
            stmt.setInt(11, this.internalCountSiblings(project.getId(), folder.getResourceId()));
            stmt.setString(12, folder.getResourceId().toString());
            stmt.executeUpdate();
            m_sqlManager.closeAll(null, stmt, null);

            // update the structure
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_STRUCTURE");
            stmt.setString(1, folder.getParentStructureId().toString());
            stmt.setString(2, folder.getResourceId().toString());
            stmt.setString(3, folder.getName());
            stmt.setInt(4, resourceState);
            stmt.setString(5, folder.getStructureId().toString());
            stmt.executeUpdate();

            //m_sqlManager.commit(conn);
        } catch (SQLException e) {
            //m_sqlManager.rollback(conn, savepoint);
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            //m_sqlManager.releaseSavepoint(conn, savepoint);
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Writes a couple of Properties for a file or folder.
     *
     * @param propertyinfos A Hashtable with propertydefinition- property-pairs as strings.
     * @param projectId The id of the current project.
     * @param resource The CmsResource object of the resource that gets the properties.
     * @param resourceType The Type of the resource.
     * @param addDefinition If <code>true</code> then the propertydefinition is added if it not exists
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public void writeProperties(Map propertyinfos, int projectId, CmsResource resource, int resourceType, boolean addDefinition) throws CmsException {
        // get all metadefs
        Iterator keys = propertyinfos.keySet().iterator();
        // one metainfo-name:
        String key;

        while (keys.hasNext()) {
            key = (String) keys.next();
            writeProperty(key, projectId, (String) propertyinfos.get(key), resource, resourceType, addDefinition);
        }
    }

    /**
     * Writes a property for a file or folder.
     *
     * @param meta The property-name of which the property has to be read.
     * @param value The value for the property to be set.
     * @param resourceId The id of the resource.
     * @param resourceType The Type of the resource.
     * @param addDefinition If <code>true</code> then the propertydefinition is added if it not exists
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public void writeProperty(String meta, int projectId, String value, CmsResource resource, int resourceType, boolean addDefinition) throws CmsException {
        CmsPropertydefinition propdef = null;
        try {
            propdef = readPropertydefinition(meta, 0, resourceType);
        } catch (CmsException ex) {
            // do nothing
        }
        String resourceName = resource.getRootPath();
        // hack: this never should happen, but it does.......
        if ((resource.isFolder()) && (!resourceName.endsWith("/"))) {
            resourceName += "/";
        }

        if (propdef == null) {
            // there is no propertydefinition for with the overgiven name for the resource
            // add this definition or throw an exception
            if (addDefinition) {
                createPropertydefinition(meta, projectId, resourceType);
            } else {
                throw new CmsException("[" + this.getClass().getName() + ".writeProperty/1] " + meta, CmsException.C_NOT_FOUND);
            }
        } else {
            // write the property into the db
            PreparedStatement stmt = null;
            Connection conn = null;
            try {
                conn = m_sqlManager.getConnection(projectId);
                if (readProperty(propdef.getName(), projectId, resource, resourceType) != null) {
                    // property exists already - use update.
                    // create statement
                    stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_PROPERTIES_UPDATE");
                    stmt.setString(1, m_sqlManager.validateNull(value));
                    stmt.setString(2, resource.getResourceId().toString());
                    stmt.setString(3, resourceName);
                    stmt.setInt(4, propdef.getId());
                    stmt.executeUpdate();
                } else {
                    // property dosen't exist - use create.
                    // create statement
                    stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_PROPERTIES_CREATE");
                    stmt.setInt(1, m_sqlManager.nextId(m_sqlManager.get(projectId, "C_TABLE_PROPERTIES")));
                    stmt.setInt(2, propdef.getId());
                    stmt.setString(3, resource.getResourceId().toString());
                    stmt.setString(4, resourceName);
                    stmt.setString(5, m_sqlManager.validateNull(value));
                    stmt.executeUpdate();
                }
            } catch (SQLException exc) {
                throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
            } finally {
                m_sqlManager.closeAll(conn, stmt, null);
            }
        }
    }
    /**
     * Updates the name of the propertydefinition for the resource type.<BR/>
     *
     * Only the admin can do this.
     *
     * @param metadef The propertydef to be written.
     * @return The propertydefinition, that was written.
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public CmsPropertydefinition writePropertydefinition(CmsPropertydefinition metadef) throws CmsException {
        PreparedStatement stmt = null;
        CmsPropertydefinition returnValue = null;
        Connection conn = null;

        try {
            for (int i = 0; i < 3; i++) {
                // write the propertydef in the offline db
                if (i == 0) {
                    conn = m_sqlManager.getConnection();
                    stmt = m_sqlManager.getPreparedStatement(conn, "C_PROPERTYDEF_UPDATE");
                }
                // write the propertydef in the online db
                else if (i == 1) {
                    conn = m_sqlManager.getConnection(I_CmsConstants.C_PROJECT_ONLINE_ID);
                    stmt = m_sqlManager.getPreparedStatement(conn, "C_PROPERTYDEF_UPDATE_ONLINE");
                }
                // write the propertydef in the backup db
                else {
                    conn = m_sqlManager.getConnectionForBackup();
                    stmt = m_sqlManager.getPreparedStatement(conn, "C_PROPERTYDEF_UPDATE_BACKUP");
                }
                stmt.setString(1, metadef.getName());
                stmt.setInt(2, metadef.getId());
                stmt.executeUpdate();
                stmt.close();
                conn.close();
            }
            // read the propertydefinition
            returnValue = readPropertydefinition(metadef.getName(), 0, metadef.getType());
        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }

        return returnValue;
    }

    /**
    * Writes a folder to the Cms.<BR/>
    *
    * @param project The project in which the resource will be used.
    * @param folder The folder to be written.
    * @param changed Flag indicating if the file state must be set to changed.
    * @param userId The user who has changed the resource
    *
    * @throws CmsException Throws CmsException if operation was not succesful.
    */
    public void writeResource(CmsProject project, CmsResource resource, byte[] filecontent, int changed, CmsUUID userId) throws CmsException {
        PreparedStatement stmt = null;
        Connection conn = null;
        long resourceDateModified = resource.isTouched() ? resource.getDateLastModified() : System.currentTimeMillis();

        boolean isFolder = false;
        //Savepoint savepoint = null;

        if (resource.getType() == CmsResourceTypeFolder.C_RESOURCE_TYPE_ID) {
            isFolder = true;
        }
        if (filecontent == null) {
            filecontent = new byte[0];
        }
        if (project.getId() == I_CmsConstants.C_PROJECT_ONLINE_ID) {
            userId = resource.getUserLastModified();
        }

        int structureState = resource.getState();
        int resourceState = resource.getState();
        if (structureState != com.opencms.core.I_CmsConstants.C_STATE_NEW && (changed > CmsDriverManager.C_NOTHING_CHANGED)) {
            if (changed == CmsDriverManager.C_UPDATE_RESOURCE_STATE)
                resourceState = com.opencms.core.I_CmsConstants.C_STATE_CHANGED;
            else if (changed == CmsDriverManager.C_UPDATE_STRUCTURE_STATE)
                structureState = com.opencms.core.I_CmsConstants.C_STATE_CHANGED;
            else
                resourceState = structureState = com.opencms.core.I_CmsConstants.C_STATE_CHANGED;
        }

        try {
            conn = m_sqlManager.getConnection(project);
            //savepoint = conn.setSavepoint("before_update");

            // update the resource
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_RESOURCES");
            stmt.setInt(1, resource.getType());
            stmt.setInt(2, resource.getFlags());
            stmt.setInt(3, resource.getLoaderId());
            stmt.setTimestamp(4, new Timestamp(resourceDateModified));
            stmt.setString(5, userId.toString());
            stmt.setInt(6, resourceState);
            stmt.setInt(7, filecontent.length);
            stmt.setString(8, resource.getFileId().toString());
            stmt.setString(9, CmsUUID.getNullUUID().toString());
            stmt.setInt(10, resource.getProjectLastModified());
            stmt.setInt(11, this.internalCountSiblings(project.getId(), resource.getResourceId()));
            stmt.setString(12, resource.getResourceId().toString());
            stmt.executeUpdate();
            m_sqlManager.closeAll(null, stmt, null);

            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UPDATE_STRUCTURE");
            stmt.setString(1, resource.getParentStructureId().toString());
            stmt.setString(2, resource.getResourceId().toString());
            stmt.setString(3, resource.getName());
            stmt.setInt(4, structureState);
            stmt.setString(5, resource.getStructureId().toString());
            stmt.executeUpdate();

            //m_sqlManager.commit(conn);            
        } catch (SQLException e) {
            //m_sqlManager.rollback(conn, savepoint);
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            //m_sqlManager.releaseSavepoint(conn, savepoint);
            m_sqlManager.closeAll(conn, stmt, null);
        }

        // write the filecontent if this is a file
        if (!isFolder) {
            this.writeFileContent(resource.getFileId(), filecontent, project.getId(), false);
        }
    }

}
