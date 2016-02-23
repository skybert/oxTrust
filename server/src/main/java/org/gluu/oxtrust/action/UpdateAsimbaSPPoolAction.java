/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.action;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.faces.context.FacesContext;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.gluu.asimba.util.ldap.sp.RequestorPoolEntry;
import org.gluu.oxtrust.ldap.service.AsimbaService;
import org.gluu.oxtrust.ldap.service.SvnSyncTimer;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.core.ResourceLoader;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Identity;
import org.xdi.config.oxtrust.ApplicationConfiguration;


/**
 * Action class for updating and adding the SAML SP to Asimba
 * 
 * @author Dmitry Ognyannikov
 */
@Scope(ScopeType.SESSION)
@Name("updateAsimbaSPPoolAction")
@Restrict("#{identity.loggedIn}")
public class UpdateAsimbaSPPoolAction implements Serializable {

    private static final long serialVersionUID = -1242167022433943680L;
    
    @Logger
    private Log log;

    @In(value = "#{oxTrustConfiguration.applicationConfiguration}")
    private ApplicationConfiguration applicationConfiguration;

    @In
    private Identity identity;

    @In
    private SvnSyncTimer svnSyncTimer;
    
    @In
    private FacesMessages facesMessages;

    @In(value = "#{facesContext}")
    private FacesContext facesContext;
    
    @In
    private ResourceLoader resourceLoader;
    
    @In
    private AsimbaService asimbaService;
    
    private RequestorPoolEntry spPool;
    
    private boolean newEntry = true;
    
    private String editEntryInum = null;
    
    private String spPoolAdditionalProperties = "";
    
    private List<RequestorPoolEntry> spPoolList = new ArrayList<RequestorPoolEntry>();
    
    @NotNull
    @Size(min = 0, max = 30, message = "Length of search string should be less than 30")
    private String searchPattern = "";
    
    public UpdateAsimbaSPPoolAction() {
        
    }
    
    @Create
    public void init() {
        log.info("init() SPPool call");
        
        clearEdit();
        
        refresh();
    }
    
    public void refresh() {
        log.info("refresh() SPPool call");
        
        if (searchPattern == null || "".equals(searchPattern)) {
            //list loading
            spPoolList = asimbaService.loadRequestorPools();
        } else {
            // search mode, clear pattern
            searchPattern = null;
        }
    }
    
    public void clearEdit() {
        spPool = new RequestorPoolEntry();
        // dafault fields
        spPool.setPostAuthorizationProfileID("postauthz.1");
        spPool.setAttributeReleasePolicyID("asimba.releasepolicy.1");
        spPool.setAuthenticationProfileIDs("remote.saml2");
        
        editEntryInum = null;
        newEntry = true;
    }
    
    @Restrict("#{s:hasPermission('trust', 'access')}")
    public void edit() {
        log.info("edit() SPPool call, inum: "+editEntryInum);
        if (editEntryInum == null || "".equals(editEntryInum)) {
            // no inum, new entry mode
            clearEdit();
        } else {
            // edit entry
            newEntry = false;
            spPool = asimbaService.readRequestorPoolEntry(editEntryInum);
        }
    }
    
    @Restrict("#{s:hasPermission('trust', 'access')}")
    public String add() {
        log.info("add new RequestorPool", spPool);
        synchronized (svnSyncTimer) {
            asimbaService.addRequestorPoolEntry(spPool);
        }
        clearEdit();
        return OxTrustConstants.RESULT_SUCCESS;
    }
    
    @Restrict("#{s:hasPermission('trust', 'access')}")
    public String update() {
        log.info("update() RequestorPool", spPool);
        synchronized (svnSyncTimer) {
            asimbaService.updateRequestorPoolEntry(spPool);
        }
        clearEdit();
        return OxTrustConstants.RESULT_SUCCESS;
    }
    
    @Restrict("#{s:hasPermission('trust', 'access')}")
    public String cancel() {
        log.info("cancel() RequestorPool", spPool);
        clearEdit();
        return OxTrustConstants.RESULT_SUCCESS;
    }
    
    @Restrict("#{s:hasPermission('person', 'access')}")
    public String delete() {
        log.info("delete() RequestorPool", spPool);
        synchronized (svnSyncTimer) {
            asimbaService.removeRequestorPoolEntry(spPool);
        }
        clearEdit();
        return OxTrustConstants.RESULT_SUCCESS;
    }
    
    @Restrict("#{s:hasPermission('person', 'access')}")
    public String search() {
        log.info("search() RequestorPool searchPattern:", searchPattern);
        synchronized (svnSyncTimer) {
            if (searchPattern != null && !"".equals(searchPattern)){
                try {
                    spPoolList = asimbaService.searchRequestorPools(searchPattern, 0);
                } catch (Exception ex) {
                    log.error("LDAP search exception", ex);
                }
            } else {
                //list loading
                spPoolList = asimbaService.loadRequestorPools();
            }
        }
        return OxTrustConstants.RESULT_SUCCESS;
    }

    /**
     * @return the spPool
     */
    public RequestorPoolEntry getSpPool() {
        return spPool;
    }

    /**
     * @param spPool the spPool to set
     */
    public void setSpPool(RequestorPoolEntry spPool) {
        this.spPool = spPool;
    }

    /**
     * @return the spPoolList
     */
    public List<RequestorPoolEntry> getSpPoolList() {
        return spPoolList;
    }

    /**
     * @param spPoolList the spPoolList to set
     */
    public void setSpPoolList(List<RequestorPoolEntry> spPoolList) {
        this.spPoolList = spPoolList;
    }

    /**
     * @return the searchPattern
     */
    public String getSearchPattern() {
        return searchPattern;
    }

    /**
     * @param searchPattern the searchPattern to set
     */
    public void setSearchPattern(String searchPattern) {
        this.searchPattern = searchPattern;
    }

    /**
     * @return the spRequestorAdditionalProperties
     */
    public String getSpPoolAdditionalProperties() {
        return spPoolAdditionalProperties;
    }
    
    public Properties getSpAdditionalPropertiesAsProperties() throws IOException {
        Properties result = new Properties();
        result.load(new StringReader(spPoolAdditionalProperties));
        return result;
    }

    /**
     * @param spPoolAdditionalProperties the spPoolAdditionalProperties to set
     */
    public void setSpPoolAdditionalProperties(String spPoolAdditionalProperties) {
        this.spPoolAdditionalProperties = spPoolAdditionalProperties;
    }
    
    public void setSpAdditionalProperties(Properties additionalProperties) {
        StringWriter writer = new StringWriter();
        for (String property : additionalProperties.stringPropertyNames()) {
            writer.write(property);
            writer.write("=");
            writer.write(additionalProperties.getProperty(property));
            writer.write("\n");
        }
        this.spPoolAdditionalProperties = writer.toString();
    }

    /**
     * @return the newEntry
     */
    public boolean isNewEntry() {
        return newEntry;
    }

    /**
     * @param newEntry the newEntry to set
     */
    public void setNewEntry(boolean newEntry) {
        this.newEntry = newEntry;
    }

    /**
     * @return the editEntryInum
     */
    public String getEditEntryInum() {
        return editEntryInum;
    }

    /**
     * @param editEntryInum the editEntryInum to set
     */
    public void setEditEntryInum(String editEntryInum) {
        this.editEntryInum = editEntryInum;
    }
}
