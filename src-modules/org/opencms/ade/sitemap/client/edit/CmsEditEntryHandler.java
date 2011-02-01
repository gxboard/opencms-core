/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/ade/sitemap/client/edit/Attic/CmsEditEntryHandler.java,v $
 * Date   : $Date: 2011/02/01 15:25:05 $
 * Version: $Revision: 1.9 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2009 Alkacon Software (http://www.alkacon.com)
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

package org.opencms.ade.sitemap.client.edit;

import org.opencms.ade.sitemap.client.Messages;
import org.opencms.ade.sitemap.client.control.CmsSitemapController;
import org.opencms.ade.sitemap.shared.CmsClientSitemapEntry;
import org.opencms.file.CmsResource;
import org.opencms.xml.sitemap.properties.CmsSimplePropertyValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The mode handler for the 'edit entry' mode of the sitemap entry editor.<p>
 * 
 * @author Georg Westenberger
 * 
 * @version $Revision: 1.9 $
 * 
 * @since 8.0.0
 */
public class CmsEditEntryHandler extends A_CmsSitemapEntryEditorHandler {

    /**
     * Creates a new instance of this class.<p>
     * 
     * @param controller the sitemap controller for this mode 
     * @param entry the sitemap entry for this mode 
     */
    public CmsEditEntryHandler(CmsSitemapController controller, CmsClientSitemapEntry entry) {

        super(controller, entry);
    }

    /**
     * @see org.opencms.ade.sitemap.client.edit.I_CmsSitemapEntryEditorHandler#getDescriptionText()
     */
    public String getDescriptionText() {

        return Messages.get().key(Messages.GUI_PROPERTY_EDITOR_TEXT_0);
    }

    /**
     * @see org.opencms.ade.sitemap.client.edit.I_CmsSitemapEntryEditorHandler#getDialogTitle()
     */
    public String getDialogTitle() {

        return Messages.get().key(Messages.GUI_EDIT_ENTRY_TITLE_0);

    }

    /**
     * @see org.opencms.ade.sitemap.client.edit.I_CmsSitemapEntryEditorHandler#getForbiddenUrlNames()
     */
    public List<String> getForbiddenUrlNames() {

        if (m_entry.getSitePath().equals("/")) {
            return Collections.<String> emptyList();
        }

        List<String> result = new ArrayList<String>();
        String parentPath = CmsResource.getParentFolder(m_entry.getSitePath());
        CmsClientSitemapEntry parent = m_controller.getEntry(parentPath);
        if (parent == null) {
            return result;
        }
        for (CmsClientSitemapEntry child : parent.getSubEntries()) {
            if (child != m_entry) {
                result.add(child.getName());
            }
        }
        return result;
    }

    /**
     * @see org.opencms.ade.sitemap.client.edit.I_CmsSitemapEntryEditorHandler#handleSubmit(java.lang.String, java.lang.String, java.lang.String, java.util.Map, boolean)
     */
    public void handleSubmit(
        String newTitle,
        String newUrlName,
        String vfsPath,
        Map<String, CmsSimplePropertyValue> fieldValues,
        boolean editedName) {

        // edit
        m_controller.editAndChangeName(m_entry, newTitle, newUrlName, vfsPath, fieldValues, editedName);

    }

    /**
     * @see org.opencms.ade.sitemap.client.edit.I_CmsSitemapEntryEditorHandler#hasEditableName()
     */
    public boolean hasEditableName() {

        return !getEntry().isRoot();
    }

    /**
     * Returns the path for the given URL name.<p>
     * 
     * @param urlName the URL name to create the path for
     * 
     * @return the new path for the given URL name
     */
    protected String getPath(String urlName) {

        if (urlName.equals("")) {
            return m_entry.getSitePath();
        }
        return CmsResource.getParentFolder(m_entry.getSitePath()) + urlName + "/";
    }

}
