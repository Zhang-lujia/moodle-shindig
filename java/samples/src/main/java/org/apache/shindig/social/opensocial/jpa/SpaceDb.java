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
import org.apache.shindig.social.opensocial.model.Space;
import org.apache.shindig.social.opensocial.model.Smoker;
import org.apache.shindig.social.opensocial.model.Url;

import org.apache.shindig.social.opensocial.jpa.MoodleCourseModuleDb;
import org.apache.shindig.social.opensocial.jpa.MoodleModuleDb;
import org.apache.shindig.social.opensocial.jpa.MoodleContextDb;

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
import javax.persistence.FetchType;

/**
 * Default Implementation of the Person object in the org.apache.shindig.social.opensocial.jpa.
 */
@Entity
@Table(name = "mdl_widgetspace") // Space is a widgetspace where widgets live, parent is a course
@NamedQueries(value = {
    @NamedQuery(name = SpaceDb.FINDBY_SPACEID,
        query = "select s from SpaceDb s where s.id = :id "),
    @NamedQuery(name = SpaceDb.FINDBY_LIKE_SPACEID,
        query = "select s from SpaceDb s where s.id like :id") })
public class SpaceDb implements Space, DbObject {

  // !!! no trailing '/'
  public static final String MOODLE_URL = "http://localhost";

  public static final String FINDBY_SPACEID = "q.space.findbyspaceid";

  public static final String PARAM_SPACEID = "id";

  public static final String FINDBY_LIKE_SPACEID = "q.space.findbylikespaceid";

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

  public static final Object JPQL_FINDSPACE = "select s from SpaceDb s where ";
  public static final Object JPQL_FINDSPACES = "select s from SpaceDb s where ";

  /**
   * The internal object ID used for references to this object. Should be generated by the
   * underlying storage mechanism
   */
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "id")
  private long objectId;
  
  /**
   * The internal parentId and parentType used for references to the parent of a space.
   */
  @Basic
  @Column(name = "course", length = 255)
  private String parentId;
  
  @Transient // parentType is a course
  protected String parentType;

  //@OneToMany(fetch=FetchType.EAGER)
  //@JoinTable(
      //name="mdl_course_modules",
      //joinColumns=
        //@JoinColumn(name="instance", referencedColumnName="id"),
      //inverseJoinColumns=@JoinColumn(name="module", referencedColumnName="id")
  //)
  //protected List<MoodleModuleDb> mm;

  @OneToMany(targetEntity = MoodleCourseModuleDb.class, mappedBy = "space", cascade = ALL)
  protected List<MoodleCourseModuleDb> courseModules;  //@OneToOne

  /**
   * An optimistic locking field.
   */

  @Transient
  protected List<Address> addresses;
  
  @Transient
  protected String location;

  @Basic
  @Column(name = "name", length = 255)
  private String displayName;

  @Basic
  @Column(name = "name", length = 255, insertable = false, updatable = false)
  private String name;
  
  @Transient
  protected String picture;
  
  
  /**
   *
   */
  @Transient
  protected List<ListField> emails;

  /**
   *
   */
  @Basic
  @Column(name = "intro", length = 255)
  protected String description;

  /**
   *
   */
  @Transient
  protected Boolean hasApp;

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
  @Transient
  protected List<String> interests;

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
  @Transient
  protected List<ListField> phoneNumbers;

  /**
   *
   */
  @Transient
  protected List<ListField> images;
  
  /**
   *
   */
  @Transient
  protected String status;

  /**
   *
   */
  @Transient
  protected Long utcOffset;

  /**
   *
   */
  @Transient
  protected List<Url> urls;

  // Note: Not in the opensocial js person object directly
  @Transient
  private boolean isOwner = false;

  public SpaceDb() {
  }

  public SpaceDb(String id, String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  public List<Address> getAddresses() {
    return addresses;
  }

  public void setAddresses(List<Address> addresses) {
    this.addresses = addresses;
  }

  public List<ListField> getEmails() {
    return emails;
  }

  public void setEmails(List<ListField> emails) {
    this.emails = emails;
  }

  public Boolean getHasApp() {
    return hasApp;
  }

  public void setHasApp(Boolean hasApp) {
    this.hasApp = hasApp;
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

  public String getLocation() {
    return this.location;
  }

  public void setLocation(String location) {
    this.location = location;
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

  public List<String> getInterests() {
    return interests;
  }

  public void setInterests(List<String> interests) {
    this.interests = interests;
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

  public List<ListField> getPhoneNumbers() {
    return phoneNumbers;
  }

  public void setPhoneNumbers(List<ListField> phoneNumbers) {
    this.phoneNumbers = phoneNumbers;
  }

  public List<ListField> getImages() {
    return images;
  }

  public void setImages(List<ListField> images) {
    this.images = images;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
  
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getUtcOffset() {
    return utcOffset;
  }

  public void setUtcOffset(Long utcOffset) {
    this.utcOffset = utcOffset;
  }

  public List<Url> getUrls() {
    return urls;
  }

  public void setUrls(List<Url> urls) {
    this.urls = urls;
  }

  public boolean getIsOwner() {
    return isOwner;
  }

  public void setIsOwner(boolean isOwner) {
    this.isOwner = isOwner;
  }

  // Proxied fields

  public String getProfileUrl() {
    for (MoodleCourseModuleDb mc : courseModules) {
      if (mc.getCourseId().equals(parentId) // course is the same as of widgetspace
         && mc.getInstanceId().equals(id) // widgetspace's id
         && mc.getModule().getName().equals("widgetspace")) {

        String uniqId = mc.getId();
        return MOODLE_URL + "/mod/widgetspace/view.php?id="+uniqId;
      }
    }

    return "Cant find widgetspace unique id";
  }

  public void setProfileUrl(String profileUrl) {
    Url url = getListFieldWithType(PROFILE_URL_TYPE, getUrls());
    if (url != null) {
      url.setValue(profileUrl);
    } else {
      setUrls(addListField(new UrlDb(profileUrl, null, PROFILE_URL_TYPE), getUrls()));
    }
  }

  public String getThumbnailUrl() {
    String pic = picture;
    if (pic == null || pic.equals("")) {
      return MOODLE_URL + "/theme/image.php?theme=standard&amp;image=icon&amp;rev=178&amp;component=widgetspace";
    }
    return MOODLE_URL + "/theme/image.php?theme=standard&amp;image=icon&amp;rev=178&amp;component=widgetspace";
  }

  public void setThumbnailUrl(String thumbnailUrl) {
    ListField photo = getListFieldWithType(THUMBNAIL_PHOTO_TYPE, getImages());
    if (photo != null) {
      photo.setValue(thumbnailUrl);
    } else {
      setImages(addListField(new ListFieldDb(THUMBNAIL_PHOTO_TYPE, thumbnailUrl), getImages()));
    }
  }

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

    Map<String, List<String>> toSave = new HashMap<String, List<String>>();
    toSave.put(INTERESTS_PROPERTY, this.interests);

  }

  @PostLoad
  public void loadTransientFields() {


    List<String> lookingFor = Lists.newArrayList();
    this.interests = Lists.newArrayList();

    Map<String, List<String>> toSave = Maps.newHashMap();

    toSave.put(INTERESTS_PROPERTY, this.interests);

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
