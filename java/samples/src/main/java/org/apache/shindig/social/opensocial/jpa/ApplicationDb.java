/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.social.opensocial.jpa;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REFRESH;
import static javax.persistence.GenerationType.IDENTITY;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.shindig.social.opensocial.jpa.api.DbObject;
import org.apache.shindig.social.opensocial.jpa.api.FilterCapability;
import org.apache.shindig.social.opensocial.jpa.api.FilterSpecification;
import org.apache.shindig.protocol.model.Enum;
import org.apache.shindig.protocol.model.FilterOperation;
import org.apache.shindig.social.opensocial.model.Account;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.BodyType;
import org.apache.shindig.social.opensocial.model.Drinker;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.LookingFor;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.NetworkPresence;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.Application;
import org.apache.shindig.social.opensocial.model.Smoker;
import org.apache.shindig.social.opensocial.model.Url;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

/**
 * Default Implementation of the Person object in the org.apache.shindig.social.opensocial.jpa.
 */
@Entity
@Table(name = "mdl_widgetspace_gadgets")
@NamedQueries(value = {
    @NamedQuery(name = ApplicationDb.FINDBY_WIDGETID,
        query = "select w from ApplicationDb w where w.id = :id "),
    @NamedQuery(name = ApplicationDb.FINDBY_LIKE_WIDGETID,
        query = "select w from ApplicationDb w where w.id like :id") })
public class ApplicationDb implements Application, DbObject {

  public static final String FINDBY_WIDGETID = "q.widget.findbywidgetid";

  public static final String PARAM_WIDGETID = "id";

  public static final String FINDBY_LIKE_WIDGETID = "q.widget.findbylikewidgetid";

  private static final String INTERESTS_PROPERTY = "interest";

  private static final Map<String, FilterSpecification> FILTER_COLUMNS =
    new HashMap<String, FilterSpecification>();

  private static final FilterOperation[] ALL_FILTEROPTIONS = new FilterOperation[] {
      FilterOperation.equals, FilterOperation.contains, FilterOperation.present,
      FilterOperation.startsWith };
  private static final FilterOperation[] NUMERIC_FILTEROPTIONS = new FilterOperation[] {
      FilterOperation.equals, FilterOperation.present };
  @SuppressWarnings("unused")
  private static final FilterOperation[] EQUALS_FILTEROPTIONS =
    new FilterOperation[] { FilterOperation.equals };

  static {
    FILTER_COLUMNS.put("displayName", new FilterSpecification("displayName", ALL_FILTEROPTIONS));

    // the following are special operations which are accepted, but work differently
    FILTER_COLUMNS.put("topFriends", new FilterSpecification());
    FILTER_COLUMNS.put("hasApp", new FilterSpecification());
  }

  private static final FilterCapability FILTER_CAPABILITY = new FilterCapability() {
    /**
     * {@inheritDoc}
     */
    public String findFilterableProperty(String fieldName, FilterOperation filterOperation) {
      FilterSpecification spec = FILTER_COLUMNS.get(fieldName);
      if (spec != null) {
        return spec.translateProperty(filterOperation);
      }
      return null;
    }

  };

  public static final String JPQL_FINDALLPERSON = null;

  // TODO The commented out query supports sorting by friend.score but needs a join with FriendDb which returns duplicates.
  // Using 'group by' to avoid duplicates doesn't work in HSQLDB or Derby - causes a "Not in aggregate function or group by clause" jdbc exception.
  // public static final String JPQL_FINDPERSON_BY_FRIENDS = "select p from PersonDb p join FriendDb f on p.objectId = f.friend.objectId where p.objectId in (select f.friend.objectId from PersonDb p, FriendDb f where p.objectId = f.person.objectId and ";
  public static final String JPQL_FINDPERSON_BY_FRIENDS = "select p from PersonDb p where p.objectId in (select f.friend.objectId from PersonDb p, FriendDb f where p.objectId = f.person.objectId and ";

  public static final Object JPQL_FINDPERSON_BY_GROUP = null;

  public static final Object JPQL_FINDWIDGET = "select w from ApplicationDb w where ";
  public static final Object JPQL_FINDWIDGETS = "select w from ApplicationDb w where ";

  /**
   * The internal object ID used for references to this object. Should be generated by the
   * underlying storage mechanism
   */
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "id")
  private long objectId;
  
  /**
   * The internal parentId and parentType used for references to the parent of a widget.
   */
  @Basic
  @Column(name = "widgetspaceid", length = 255)
  private String parentId;
  
  @Transient
  protected String parentType;

  /**
   * An optimistic locking field.
   */

  @Basic
  @Column(name = "url", length = 255)
  protected String appUrl;
  
  @Transient
  protected String appType;

  @Transient
  protected String author;
  
  @Transient
  protected String authorEmail;

  @Basic
  @Column(name = "name", length = 255)
  private String displayName;

  @Basic
  @Column(name = "name", length = 255, insertable = false, updatable = false)
  private String name;
  
  @Basic
  @Column(name = "thumbnail")
  protected String picture;
  
  
  /**
   *
   */
  @Basic
  @Column(name = "height")
  protected Integer height;

  /**
   *
   */
  @Basic
  @Column(name = "description", length = 255)
  protected String description;


  /**
   *
   */
  @Basic
  @Column(name = "id", length = 255, insertable = false, updatable = false)
  protected String id;

  /**
   *
   */
  @Transient
  protected List<ListField> ims;


  /**
   *
   */
  // @Basic
  // @Column(name = "timemodified")
  // @Temporal(TemporalType.TIMESTAMP)
  @Transient
  protected Date updated;

  /**
   *
   */
  @Basic
  @Column(name = "screenshot")
  protected String screenshotUrl;
  
  /**
   *
   */
  @Basic
  @Column(name = "thumbnail", length = 255, insertable = false, updatable = false)
  protected String thumbnailUrl;

  //Add the timemodified field to this object for updating this column in db by lujia 
  @Basic
  @Column(name = "timemodified")
  protected long timemodified;
  		
  /**
   *
   */
  @Transient
  protected Long utcOffset;

  /**
   *
   */
  @Transient
  protected List<String> tags;

  // Note: Not in the opensocial js person object directly
  @Transient
  private boolean isOwner = false;

  public ApplicationDb() {
  }

  public ApplicationDb(String id, String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  public String getAppType() {
	  return appType;
  }

  public void setAppType(String appType) {
	  this.appType = appType;
  }
	  
  public String getAppUrl() {
    return appUrl;
  }

  public void setAppUrl(String appUrl) {
    this.appUrl = appUrl;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public Integer getHeight() {
    return height;
  }

  public void setHeight(Integer height) {
    this.height = height;
  }

  public String getScreenshotUrl() {
    if (screenshotUrl == null || screenshotUrl.equals("")) {
      return "no screenshot for this widget";
    }
    return screenshotUrl;
  }

  public void setScreenshotUrl(String screenshotUrl) {
    this.screenshotUrl = screenshotUrl;
  }
  
  public String getProfileUrl() {
    return "no profile urls for apps in Moodle";
  }

  public void setProfileUrl(String profileUrl) {

  }

  public String getThumbnailUrl() {
    String pic = picture;
    if (pic == null || pic.equals("")) {
      return "no thumbnail for this widget";
    }
    
    return pic;
  }

  public void setThumbnailUrl(String thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
  }

  public String getAuthorEmail() {
    return authorEmail;
  }

  public void setAuthorEmail(String authorEmail) {
    this.authorEmail = authorEmail;
  }                               

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public String getParentId() {
    return this.parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }
  
  public String getParentType() {
    return "@space";
  }

  public void setParentType(String parentType) {
    this.parentType = parentType;
  }
  
  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<ListField> getIms() {
    return ims;
  }

  public void setIms(List<ListField> ims) {
    this.ims = ims;
  }

  public Date getUpdated() {
    if (updated == null) {
      return null;
    }
    return new Date(updated.getTime());
  }

  public void setUpdated(Date updated) {
    if (updated == null) {
      this.updated = null;
    } else {
      this.updated = new Date(updated.getTime());
    }
  }


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
  
  public Long getUtcOffset() {
    return utcOffset;
  }

  public void setUtcOffset(Long utcOffset) {
    this.utcOffset = utcOffset;
  }

  public boolean getIsOwner() {
    return isOwner;
  }

  public void setIsOwner(boolean isOwner) {
    this.isOwner = isOwner;
  }

  // for update the timemodified field
  public void setTimeModified(long timemodified) {
    this.timemodified = timemodified;
  }
  
  public long getTimeModified() {
    return this.timemodified;
  }
  
  
  // Proxied fields
	


  private <T extends ListField> T getListFieldWithType(String type, List<T> list) {
    if (list != null) {
      for (T url : list) {
        if (type.equalsIgnoreCase(url.getType())) {
          return url;
        }
      }
    }

    return null;
  }

  private <T extends ListField> List<T> addListField(T field, List<T> list) {
    if (list == null) {
      list = Lists.newArrayList();
    }
    list.add(field);
    return list;
  }

  /**
   * @return the objectId
   */
  public long getObjectId() {
    return Long.parseLong(id);
  }

  @PrePersist
  public void populateDbFields() {


  }

  @PostLoad
  public void loadTransientFields() {


  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.shindig.social.opensocial.model.Person#getDisplayName()
   */
  public String getDisplayName() {
    return displayName;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.shindig.social.opensocial.model.Person#setDisplayName(java.lang.String)
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public static FilterCapability getFilterCapability() {
    return FILTER_CAPABILITY;

  }

  /** {@inheritDoc} */
  public Map<String, ? extends Object> getAppData()
  {
    return null;
  }

  /** {@inheritDoc} */
  public void setAppData( Map<String, ? extends Object> appData )
  {
  }
}
