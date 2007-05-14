/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/I_CmsProjectDriver.java,v $
 * Date   : $Date: 2007/05/14 13:10:16 $
 * Version: $Revision: 1.76.4.10 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
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
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.db;

import org.opencms.db.generic.CmsSqlManager;
import org.opencms.file.CmsDataAccessException;
import org.opencms.file.CmsFile;
import org.opencms.file.CmsFolder;
import org.opencms.file.CmsGroup;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsUser;
import org.opencms.file.CmsProject.CmsProjectType;
import org.opencms.main.CmsException;
import org.opencms.publish.CmsPublishJobInfoBean;
import org.opencms.report.I_CmsReport;
import org.opencms.util.CmsUUID;

import java.util.List;
import java.util.Set;

/**
 * Definitions of all required project driver methods. <p>
 * 
 * @author Thomas Weckert 
 * @author Michael Emmerich 
 * 
 * @version $Revision: 1.76.4.10 $
 * 
 * @since 6.0.0 
 */
public interface I_CmsProjectDriver {

    /** Name of the setup project. */
    String SETUP_PROJECT_NAME = "_setupProject";

    /** The type ID to identify project driver implementations. */
    int DRIVER_TYPE_ID = 1;

    /** The name of the temp file project. */
    String TEMP_FILE_PROJECT_NAME = "tempFileProject";

    /**
     * Creates a new project.<p>
     * 
     * @param dbc the current database context
     * @param id the project id
     * @param owner the owner of the project
     * @param group the group for the project
     * @param managergroup the managergroup for the project
     * @param name the name of the project to create
     * @param description the description for the project
     * @param flags the flags for the project
     * @param type the type for the project
     * 
     * @return the created <code>{@link CmsProject}</code> instance
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    CmsProject createProject(
        CmsDbContext dbc,
        CmsUUID id,
        CmsUser owner,
        CmsGroup group,
        CmsGroup managergroup,
        String name,
        String description,
        int flags,
        CmsProjectType type) throws CmsDataAccessException;

    /**
     * Creates a new projectResource from a given CmsResource object.<p>
     *
     * @param dbc the current database context
     * @param projectId The project in which the resource will be used
     * @param resourceName The resource to be written to the Cms
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    void createProjectResource(CmsDbContext dbc, CmsUUID projectId, String resourceName) throws CmsDataAccessException;

    /**
     * Inserts an entry for a publish job .<p>
     * 
     * @param dbc the current database context
     * @param publishJob the publish job data
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    void createPublishJob(CmsDbContext dbc, CmsPublishJobInfoBean publishJob) throws CmsDataAccessException;

    /**
     * Deletes all entries in the published resource table.<p>
     * 
     * @param dbc the current database context
     * @param linkType the type of resource deleted (0= non-paramter, 1=parameter)
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    void deleteAllStaticExportPublishedResources(CmsDbContext dbc, int linkType)
    throws CmsDataAccessException;

    /**
     * Deletes a project from the cms.<p>
     * 
     * Therefore it deletes all files, resources and properties.
     * 
     * @param dbc the current database context
     * @param project the project to delete
     * @throws CmsDataAccessException if something goes wrong
     */
    void deleteProject(CmsDbContext dbc, CmsProject project) throws CmsDataAccessException;

    /**
     * Delete a projectResource from an given CmsResource object.<p>
     *
     * @param dbc the current database context
     * @param projectId id of the project in which the resource is used
     * @param resourceName name of the resource to be deleted from the Cms
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    void deleteProjectResource(CmsDbContext dbc, CmsUUID projectId, String resourceName) throws CmsDataAccessException;

    /**
     * Deletes a specified project.<p>
     * 
     * @param dbc the current database context
     * @param project the project to be deleted
     *
     * @throws CmsDataAccessException if operation was not succesful
     */
    void deleteProjectResources(CmsDbContext dbc, CmsProject project) throws CmsDataAccessException;

    /**
     * Deletes all publish history entries with publish tags >=0 and < the specified max. publish tag.<p>
     * 
     * @param dbc the current database context
     * @param projectId the ID of the current project
     * @param maxPublishTag entries with publish tags >=0 and < this max. publish tag get deleted
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    void deletePublishHistory(CmsDbContext dbc, CmsUUID projectId, int maxPublishTag) throws CmsDataAccessException;

    /**
     * Deletes a publish history entry with publish tags >=0 and < the specified max. publish tag.<p>
     * 
     * @param dbc the current database context
     * @param publishHistoryId the id of the history to delete the entry from
     * @param publishResource the entry to delete
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    void deletePublishHistoryEntry(
        CmsDbContext dbc,
        CmsUUID publishHistoryId,
        CmsPublishedResource publishResource) throws CmsDataAccessException;

    /**
     * Deletes a publish job identified by its history id.<p>
     * 
     * @param dbc the current database context
     * @param publishHistoryId the history id identifying the publish job
     * @throws CmsDataAccessException if something goes wrong
     */
    void deletePublishJob(CmsDbContext dbc, CmsUUID publishHistoryId) throws CmsDataAccessException;

    /**
     * Deletes the publish list assigned to a publish job.<p>
     * 
     * @param dbc the current database context 
     * @param publishHistoryId the history id identifying the publish job
     * @throws CmsDataAccessException if something goes wrong
     */
    void deletePublishList(CmsDbContext dbc, CmsUUID publishHistoryId) throws CmsDataAccessException;

    /**
     * Deletes an entry in the published resource table.<p>
     * 
     * @param dbc the current database context
     * @param resourceName The name of the resource to be deleted in the static export
     * @param linkType the type of resource deleted (0= non-paramter, 1=parameter)
     * @param linkParameter the parameters of the resource
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    void deleteStaticExportPublishedResource(
        CmsDbContext dbc,
        String resourceName,
        int linkType,
        String linkParameter) throws CmsDataAccessException;

    /**
     * Destroys this driver.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    void destroy() throws Throwable;

    /**
     * Fills the OpenCms database tables with default values.<p>
     * 
     * @param dbc the current database context
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    void fillDefaults(CmsDbContext dbc) throws CmsDataAccessException;

    /**
     * Returns the SqlManager of this driver.<p>
     * 
     * @return the SqlManager of this driver
     */
    CmsSqlManager getSqlManager();

    /**
     * Initializes the SQL manager for this driver.<p>
     * 
     * To obtain JDBC connections from different pools, further 
     * {online|offline|history} pool Urls have to be specified.<p>
     * 
     * @param classname the classname of the SQL manager
     * 
     * @return the SQL manager for this driver
     */
    org.opencms.db.generic.CmsSqlManager initSqlManager(String classname);

    /**
     * Publishes a deleted folder.<p>
     * 
     * @param dbc the current database context
     * @param report the report to log the output to
     * @param m the number of the folder to publish
     * @param n the number of all folders to publish
     * @param onlineProject the online project
     * @param offlineFolder the offline folder to publish
     * @param historyEnabled flag if history is enabled
     * @param publishDate the publishing date
     * @param publishHistoryId the publish history id
     * @param publishTag the publish tag
     * @param maxVersions the maxmum number of historical versions for each resource
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    void publishDeletedFolder(
        CmsDbContext dbc,
        I_CmsReport report,
        int m,
        int n,
        CmsProject onlineProject,
        CmsFolder offlineFolder,
        boolean historyEnabled,
        long publishDate,
        CmsUUID publishHistoryId,
        int publishTag,
        int maxVersions) throws CmsDataAccessException;

    /**
     * Publishes a new, changed or deleted file.<p>
     * 
     * @param dbc the current database context
     * @param report the report to log the output to
     * @param m the number of the file to publish
     * @param n the number of all files to publish
     * @param onlineProject the online project
     * @param offlineResource the offline file to publish
     * @param publishedContentIds contains the UUIDs of already published content records
     * @param historyEnabled flag if history is enabled
     * @param publishDate the publishing date
     * @param publishHistoryId the publish history id
     * @param publishTag the publish tag
     * @param maxVersions the maxmum number of historical versions for each resource
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    void publishFile(
        CmsDbContext dbc,
        I_CmsReport report,
        int m,
        int n,
        CmsProject onlineProject,
        CmsResource offlineResource,
        Set publishedContentIds,
        boolean historyEnabled,
        long publishDate,
        CmsUUID publishHistoryId,
        int publishTag,
        int maxVersions) throws CmsDataAccessException;

    /**
     * Publishes the content record of a file.<p>
     * 
     * The content record is only published unless it's UUID is not contained in publishedContentIds.
     * The calling method has to take care about whether an existing content record has to be deleted 
     * before or not.<p>  
     * 
     * The intention of this method is to get overloaded in a project driver
     * for a specific DB server to shift the binary content from the offline into the online table
     * in a more sophisticated way than in the generic ANSI-SQL implementation of this interface.
     * 
     * @param dbc the current database context
     * @param offlineProject the offline project to read data
     * @param onlineProject the online project to write data
     * @param offlineFileHeader the offline header of the file of which the content gets published
     * @param publishedResourceIds a Set with the UUIDs of the already published content records
     * @param needToUpdateContent <code>true</code> if the content record has to be updated
     * @param publishTag the publish tag
     * 
     * @return the published file (online)
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    CmsFile publishFileContent(
        CmsDbContext dbc,
        CmsProject offlineProject,
        CmsProject onlineProject,
        CmsResource offlineFileHeader,
        Set publishedResourceIds,
        boolean needToUpdateContent,
        int publishTag) throws CmsDataAccessException;

    /**
     * Publishes a new or changed folder.<p>
     * 
     * @param dbc the current database context
     * @param report the report to log the output to
     * @param m the number of the folder to publish
     * @param n the number of all folders to publish
     * @param onlineProject the online project
     * @param currentFolder the offline folder to publish
     * @param historyEnabled flag if history is enabled
     * @param publishDate the publishing date
     * @param publishHistoryId the publish history id
     * @param publishTag the publish tag
     * @param maxVersions the maxmum number of historical versions for each resource
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    void publishFolder(
        CmsDbContext dbc,
        I_CmsReport report,
        int m,
        int n,
        CmsProject onlineProject,
        CmsFolder currentFolder,
        boolean historyEnabled,
        long publishDate,
        CmsUUID publishHistoryId,
        int publishTag,
        int maxVersions) throws CmsDataAccessException;

    /**
     * Publishes a specified project to the online project.<p>
     * 
     * @param dbc the current database context
     * @param report an I_CmsReport instance to print output messages
     * @param onlineProject the online project
     * @param publishList a Cms publish list
     * @param historyEnabled true if published resources should be written to the historical archive
     * @param publishTag the publish tag
     * @param maxVersions maximum number of historical versions
     * 
     * @throws CmsException if something goes wrong
     */
    void publishProject(
        CmsDbContext dbc,
        I_CmsReport report,
        CmsProject onlineProject,
        CmsPublishList publishList,
        boolean historyEnabled,
        int publishTag,
        int maxVersions) throws CmsException;

    /**
     * Reads the <code>{@link List}&lt{@link org.opencms.lock.CmsLock};&gt; </code> 
     * that were saved to the database in the previous run of OpenCms.<p>
     * 
     * @param dbc the current database context
     * 
     * @return the <code>{@link List}&lt{@link org.opencms.lock.CmsLock};&gt; </code> 
     *      that were saved to the database in the previous run of OpenCms.
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    List readLocks(CmsDbContext dbc) throws CmsDataAccessException;

    /**
     * Reads a project given the projects id.<p>
     *
     * @param dbc the current database context
     * @param id the id of the project
     * 
     * @return the project read
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    CmsProject readProject(CmsDbContext dbc, CmsUUID id) throws CmsDataAccessException;

    /**
     * Reads a project.<p>
     *
     * @param dbc the current database context
     * @param name the name of the project
     * 
     * @return the project with the given name
     * @throws CmsDataAccessException if something goes wrong
     */
    CmsProject readProject(CmsDbContext dbc, String name) throws CmsDataAccessException;

    /**
     * Reads the project resource path for a given project and resource path,
     * to validate if a resource path for a given project already exists.<p>
     * 
     * @param dbc the current database context
     * @param projectId the ID of the project for which the resource path is read
     * @param resourcename the project's resource path
     * 
     * @return String the project's resource path
     * @throws CmsDataAccessException if something goes wrong
     */
    String readProjectResource(CmsDbContext dbc, CmsUUID projectId, String resourcename) throws CmsDataAccessException;

    /**
     * Reads the project resources for a specified project.<p>
     * 
     * @param dbc the current database context
     * @param project the project for which the resource path is read
     * 
     * @return a list of all project resource paths
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    List readProjectResources(CmsDbContext dbc, CmsProject project) throws CmsDataAccessException;

    /**
     * Returns all projects in the given organizational unit.<p>
     *
     * @param dbc the current database context
     * @param ouFqn the fully qualified name of the organizational unit to get the projects for
     * 
     * @return a list of objects of type <code>{@link CmsProject}</code>
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    List readProjects(CmsDbContext dbc, String ouFqn) throws CmsDataAccessException;

    /**
     * Returns all projects, which are accessible by a group.<p>
     *
     * @param dbc the current database context
     * @param group the requesting group
     * 
     * @return a Vector of projects
     * @throws CmsDataAccessException if something goes wrong
     */
    List readProjectsForGroup(CmsDbContext dbc, CmsGroup group) throws CmsDataAccessException;

    /**
     * Returns all projects, which are manageable by a group.<p>
     *
     * @param dbc the current database context
     * @param group The requesting group
     * @return a Vector of projects
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    List readProjectsForManagerGroup(CmsDbContext dbc, CmsGroup group) throws CmsDataAccessException;

    /**
     * Reads all projects which are owned by a specified user.<p>
     *
     * @param dbc the current database context
     * @param user the user
     * 
     * @return a list of objects of type <code>{@link CmsProject}</code>
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    List readProjectsForUser(CmsDbContext dbc, CmsUser user) throws CmsDataAccessException;

    /**
     * Reads a single publish job identified by its publish history id.<p>
     * 
     * @param dbc the current database context
     * @param publishHistoryId unique id to identify the publish job in the publish history
     * @return an object of type <code>{@link CmsPublishJobInfoBean}</code> 
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    CmsPublishJobInfoBean readPublishJob(CmsDbContext dbc, CmsUUID publishHistoryId) throws CmsDataAccessException;

    /**
     * Reads all publish jobs finished in the given time range.<p>
     * If <code>(0L, 0L)</code> is passed as time range, all pending jobs are returned.
     * 
     * @param dbc the current database context
     * @param startTime the start of the time range for finish time
     * @param endTime the end of the time range for finish time
     * @return a list of objects of type <code>{@link CmsPublishJobInfoBean}</code>
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    List readPublishJobs(CmsDbContext dbc, long startTime, long endTime) throws CmsDataAccessException;

    /**
     * Reads the publish list assigned to a publish job.<p>
     * 
     * @param dbc the current database context
     * @param publishHistoryId the history id identifying the publish job
     * @return the assigned publish list
     * @throws CmsDataAccessException if something goes wrong
     */
    CmsPublishList readPublishList(CmsDbContext dbc, CmsUUID publishHistoryId) throws CmsDataAccessException;

    /**
     * Reads the publish report assigned to a publish job.<p>
     * 
     * @param dbc the current database context
     * @param publishHistoryId the history id identifying the publish job  
     * @return the content of the assigned publish report
     * @throws CmsDataAccessException if something goes wrong
     */
    byte[] readPublishReportContents(CmsDbContext dbc, CmsUUID publishHistoryId) throws CmsDataAccessException;

    /**
     * Reads the resources that were published during a publish process for a given publish history ID.<p>
     * 
     * @param dbc the current database context
     * @param publishHistoryId unique int ID to identify the publish process in the publish history
     * 
     * @return a list of <code>{@link org.opencms.db.CmsPublishedResource}</code> objects
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    List readPublishedResources(CmsDbContext dbc, CmsUUID publishHistoryId)
    throws CmsDataAccessException;

    /**
     * Returns the parameters of a resource in the table of all published template resources.<p>
     *
     * @param dbc the current database context
     * @param rfsName the rfs name of the resource
     * 
     * @return the paramter string of the requested resource
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    String readStaticExportPublishedResourceParameters(CmsDbContext dbc, String rfsName)
    throws CmsDataAccessException;

    /**
     * Returns a list of all template resources which must be processed during a static export.<p>
     * 
     * @param dbc the current database context
     * @param parameterResources flag for reading resources with parameters (1) or without (0)
     * @param timestamp the timestamp information
     * 
     * @return a list of template resources as <code>{@link String}</code> objects
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    List readStaticExportResources(CmsDbContext dbc, int parameterResources, long timestamp)
    throws CmsDataAccessException;

    /**
     * Removes the project id from all resources within a project.<p>
     * 
     * This must be done when a project will deleted
     * 
     * @param dbc the current database context
     * @param project the project to delete
     * @throws CmsDataAccessException if something goes wrong
     */
    void unmarkProjectResources(CmsDbContext dbc, CmsProject project) throws CmsDataAccessException;

    /**
     * Writes the <code>{@link List}&lt{@link org.opencms.lock.CmsLock};&gt; </code> 
     * to the database for reuse in the next run of OpenCms.<p>   
     * 
     * This method must only be called at startup or the in-memory locking will overwritten.<p>
     * 
     * @param dbc the current database context
     * 
     * @param locks the <code>{@link List}&lt{@link org.opencms.lock.CmsLock};&gt;</code> that 
     *      currently exist in OpenCms ({@link org.opencms.lock.CmsLockManager}) 
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    void writeLocks(CmsDbContext dbc, List locks) throws CmsDataAccessException;

    /**
     * Writes an already existing project.<p>
     *
     * The project id has to be a valid OpenCms project id.<br>
     * 
     * The project with the given id will be completely overriden
     * by the given data.<p>
     *
     * @param dbc the current database context
     * @param project the project that should be written
     * 
     * @throws CmsDataAccessException if operation was not successful
     */
    void writeProject(CmsDbContext dbc, CmsProject project) throws CmsDataAccessException;

    /**
     * Inserts an entry in the publish history for a published VFS resource.<p>
     * 
     * @param dbc the current database context
     * @param publishId the ID of the current publishing process
     * @param resource the state of the resource *before* it was published
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    void writePublishHistory(
        CmsDbContext dbc,
        CmsUUID publishId,
        CmsPublishedResource resource) throws CmsDataAccessException;

    /**
     * Writes a publish job.<p>
     * 
     * @param dbc the current database context
     * @param publishJob the publish job to write
     * @throws CmsDataAccessException if something goes wrong
     */
    void writePublishJob(CmsDbContext dbc, CmsPublishJobInfoBean publishJob) throws CmsDataAccessException;

    /**
     * Writes a publish report for a publish job.<p>
     * 
     * @param dbc the current database context
     * @param publishId the ID of the current publishing process
     * @param content the report output
     * @throws CmsDataAccessException if something goes wrong
     */
    void writePublishReport(CmsDbContext dbc, CmsUUID publishId, byte[] content) throws CmsDataAccessException;

    /**
     * Inserts an entry in the published resource table.<p>
     * 
     * This is done during static export.<p>
     * 
     * @param dbc the current database context
     * @param resourceName The name of the resource to be added to the static export
     * @param linkType the type of resource exported (0= non-paramter, 1=parameter)
     * @param linkParameter the parameters added to the resource
     * @param timestamp a timestamp for writing the data into the db
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    void writeStaticExportPublishedResource(
        CmsDbContext dbc,
        String resourceName,
        int linkType,
        String linkParameter,
        long timestamp) throws CmsDataAccessException;

}