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
package org.apache.shindig.social.opensocial.jpa.spi;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.gadgets.servlet.JsonRpcHandler;
import org.apache.shindig.gadgets.servlet.RpcException;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.social.opensocial.jpa.PersonDb;
import org.apache.shindig.social.opensocial.jpa.ApplicationDb;
import org.apache.shindig.social.opensocial.jpa.api.FilterCapability;
import org.apache.shindig.social.opensocial.jpa.api.FilterSpecification;
import org.apache.shindig.social.opensocial.jpa.spi.JPQLUtils;
import org.apache.shindig.social.opensocial.jpa.spi.SPIUtils;
import org.apache.shindig.social.opensocial.model.Application;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.ApplicationService;
import org.apache.shindig.social.opensocial.spi.Context;
import org.apache.shindig.social.opensocial.spi.ApplicationId;
import org.apache.shindig.social.opensocial.spi.ApplicationUrl;
import org.apache.shindig.social.opensocial.spi.UserId;

import org.json.JSONException;
import org.json.JSONObject;
import com.google.inject.Inject;

import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.*;


import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletResponse;

/**
 * Implements the PersonService from the SPI binding to the JPA model and providing queries to
 * support the OpenSocial implementation.
 */
public class ApplicationServiceDb implements ApplicationService {

  /**
   * This is the JPA entity manager, shared by all threads accessing this service (need to check
   * that its really thread safe).
   */
  private EntityManager entityManager;
  private JsonRpcHandler jsonHandler;

  /**
   * Create the PersonServiceDb, injecting an entity manager that is configured with the social
   * model.
   *
   * @param entityManager the entity manager containing the social model.
   */
  @Inject
  public ApplicationServiceDb(EntityManager entityManager,
      JsonRpcHandler jsonHandler) {
    this.entityManager = entityManager;
    this.jsonHandler = jsonHandler;
  }

  /**
   * {@inheritDoc}
   */
  public Future<RestfulCollection<Application>> getApplications(Set<ApplicationId> applicationIds, 
		  CollectionOptions collectionOptions, Set<String> fields,
       SecurityToken token) throws ProtocolException {
    // for each user id get the filtered userid using the token and then, get the users identified
    // by the group id, the final set is filtered
    // using the collectionOptions and return the fields requested.

    // not dealing with the collection options at the moment, and not the fields because they are
    // either lazy or at no extra costs, the consumer will either access the properties or not
    List<Application> plist = null;
    int lastPos = 1;
    Long totalResults = null;

    StringBuilder sb = new StringBuilder();
    // sanitize the list to get the uid's and remove duplicates
    List<String> paramList = SPIUtils.getApplicationList(applicationIds);
    
    sb.append(ApplicationDb.JPQL_FINDWIDGET);
    lastPos = JPQLUtils.addInClause(sb, "w", "id", lastPos, paramList.size());

    
    // Get total results, that is count the total number of rows for this query
    // totalResults = JPQLUtils.getTotalResults(entityManager, sb.toString(), paramList);

    
    // Execute ordered and paginated query
    //if (totalResults > 0) {
    	//addOrderClause(sb, collectionOptions);
    	plist = JPQLUtils.getListQuery(entityManager, sb.toString(), paramList, collectionOptions);
    //}

    if (plist == null) {
    	plist = Lists.newArrayList();
    }
    // FIXME: use JPQLUtils.getTotalResults for it
    totalResults = new Long(plist.size());
    // all of the above could equally have been placed into a thread to overlay the
    // db wait times.
    RestfulCollection<Application> restCollection = new RestfulCollection<Application>(
        plist, collectionOptions.getFirst(), totalResults.intValue(), collectionOptions.getMax());
    return ImmediateFuture.newInstance(restCollection);

  }

  public Future<RestfulCollection<Application>> getApplicationsForContext(Context context, 
		  CollectionOptions collectionOptions, Set<String> fields,
       SecurityToken token) throws ProtocolException {
    // list of applications is retrieved for a context

    // not dealing with the collection options at the moment, and not the fields because they are
    // either lazy or at no extra costs, the consumer will either access the properties or not
    List<Application> plist = null;
    int lastPos = 1;
    Long totalResults = null;

    StringBuilder sb = new StringBuilder();
    // sanitize the list to get the uid's and remove duplicates
    List<String> paramList = Lists.newArrayList();
    
    sb.append(ApplicationDb.JPQL_FINDWIDGETS);
    if(context.getContextType().equals("@person")){
    	sb.append("w.parentId = "+context.getContextId()+" and w.parentType = 'User'");
    }else if (context.getContextType().equals("@space")){
    	sb.append("w.parentId = "+context.getContextId()+" and w.parentType = 'Space'");
    }
    
    // Get total results, that is count the total number of rows for this query
    // totalResults = JPQLUtils.getTotalResults(entityManager, sb.toString(), paramList);
    
    // Execute ordered and paginated query
    //if (totalResults > 0) {
    	//addOrderClause(sb, collectionOptions);
    	plist = JPQLUtils.getListQuery(entityManager, sb.toString(), paramList, collectionOptions);
    //}

    if (plist == null) {
    	plist = Lists.newArrayList();
    }
    // FIXME: use JPQLUtils.getTotalResults for it
    totalResults = new Long(plist.size());
    // all of the above could equally have been placed into a thread to overlay the
    // db wait times.
    RestfulCollection<Application> restCollection = new RestfulCollection<Application>(
        plist, collectionOptions.getFirst(), totalResults.intValue(), collectionOptions.getMax());
    return ImmediateFuture.newInstance(restCollection);

  }

  /**
   * {@inheritDoc}
   */
  public Future<Application> getApplication(ApplicationId applicationId, Set<String> fields, SecurityToken token)
      throws ProtocolException {
 
	Query q = null;
	// gets application for applicationId from the database
	q = entityManager.createNamedQuery(ApplicationDb.FINDBY_WIDGETID);
	q.setParameter(ApplicationDb.PARAM_WIDGETID, applicationId.getApplicationId());
	q.setFirstResult(0);
	q.setMaxResults(1);
		

    List<?> plist = q.getResultList();
    Application application = null;
    if (plist != null && !plist.isEmpty()) {
      application = (Application) plist.get(0);
    }
    return ImmediateFuture.newInstance(application);
  }

  public Future<Void> createApplication(ApplicationUrl applicationUrl,
      SecurityToken token) throws ProtocolException {
    String data = "{\"context\":{\"view\":\"canvas\",\"container\":\"default\"},\"gadgets\":[{\"url\":\""
        + applicationUrl.getApplicationUrl() + "\", \"moduleId\":0}]}";

    // use the rpc method to fetch the metadata of a widget
    JSONObject resp;
    try {
    resp = jsonHandler.process(new JSONObject(data)).getJSONArray("gadgets").getJSONObject(0);

    if (!entityManager.getTransaction().isActive()) {
    entityManager.getTransaction().begin();
    }
	
	// when inserting the record with native query there is no exception when the page in moodle reloads 
    /*Query query = entityManager
        .createNativeQuery("INSERT INTO mdl_widgetspace_gadgets (url, widgetspaceid, name, height, thumbnail, screenshot, description, timemodified) "
            + " VALUES(?,?,?,?,?,?,?,?)");
    query.setParameter(1, applicationUrl.getApplicationUrl());
    query.setParameter(2, token.getOwnerId().replace("s_", ""));
    query.setParameter(3, resp.get("title").toString());
    int widget_height = Integer.parseInt(resp.get("height").toString());
    widget_height = (widget_height == 0) ? 200 : widget_height;
    query.setParameter(4, widget_height);
    query.setParameter(5, resp.get("thumbnail").toString());
    query.setParameter(6, resp.get("screenshot").toString());
    query.setParameter(7, resp.get("description").toString());
	query.setParameter(8, Calendar.getInstance().getTimeInMillis()/1000);
    query.executeUpdate();

    entityManager.getTransaction().commit();*/

	//the question is that if using the jpa instead of native query then there arise an exception
    
    if (!entityManager.getTransaction().isActive()) {
    entityManager.getTransaction().begin(); } 
	ApplicationDb applicationDb = new ApplicationDb();
    applicationDb.setParentId(token.getOwnerId().replace("s_", ""));
    applicationDb.setAppUrl(applicationUrl.getApplicationUrl());
    applicationDb.setDisplayName(resp.get("title").toString());
     
    int widget_height = Integer.parseInt(resp.get("height").toString());
    widget_height = (widget_height == 0) ? 200 : widget_height;
    applicationDb.setHeight(widget_height);
    applicationDb.setThumbnailUrl(resp.get("thumbnail").toString());
    applicationDb.setScreenshotUrl(resp.get("screenshot").toString());
    applicationDb.setDescription(resp.get("description").toString());
    
    applicationDb.setTimeModified(Calendar.getInstance().getTimeInMillis() / 1000);
    entityManager.persist(applicationDb);
    entityManager.getTransaction().commit();

    } catch (RpcException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
    } catch (JSONException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
    }
    return ImmediateFuture.newInstance(null);
  }

  /**
   * Add a filter clause specified by the collection options.
   *
   * @param sb the query builder buffer
   * @param collectionOptions the options
   * @param lastPos the last positional parameter that was used so far in the query
   * @return
   */
  private int addFilterClause(StringBuilder sb, FilterCapability filterable,
      CollectionOptions collectionOptions, int lastPos) {
    // this makes the filter value saf
    String filter = filterable.findFilterableProperty(collectionOptions.getFilter(),
        collectionOptions.getFilterOperation());
    String filterValue = collectionOptions.getFilterValue();
    int filterPos = 0;
    if (FilterSpecification.isValid(filter)) {
      if (FilterSpecification.isSpecial(filter)) {
        if (ApplicationService.HAS_APP_FILTER.equals(filter)) {
          // Retrieves all friends with any data for this application.
          // TODO: how do we determine which application is being talked about,
          // the assumption below is wrong
          filterPos = lastPos + 1;
          sb.append(" f.application_id  = ?").append(filterPos);
        } else if (ApplicationService.TOP_FRIENDS_FILTER.equals(filter)) {
          // Retrieves only the user's top friends, this is defined here by the implementation
          // and there is an assumption that the sort order has already been applied.
          // to do this we need to modify the collections options
          // there will only ever b x friends in the list and it will only ever start at 1

          collectionOptions.setFirst(1);
          collectionOptions.setMax(20);

        } else if (ApplicationService.ALL_FILTER.equals(filter)) {
           // select all, ie no filtering
        } else if (ApplicationService.IS_WITH_FRIENDS_FILTER.equals(filter)) {
          filterPos = lastPos + 1;
          sb.append(" f.friend  = ?").append(filterPos);
        }
      } else {
        sb.append("p.").append(filter);
        switch (collectionOptions.getFilterOperation()) {
        case contains:
          filterPos = lastPos + 1;
          sb.append(" like ").append(" ?").append(filterPos);
          filterValue = '%' + filterValue + '%';
          collectionOptions.setFilter(filterValue);
          break;
        case equals:
          filterPos = lastPos + 1;
          sb.append(" = ").append(" ?").append(filterPos);
          break;
        case present:
          sb.append(" is not null ");
          break;
        case startsWith:
          filterPos = lastPos + 1;
          sb.append(" like ").append(" ?").append(filterPos);
          filterValue = '%' + filterValue + '%';
          collectionOptions.setFilter(filterValue);
          break;
        }
      }
    }
    return filterPos;
  }

  /**
   * Add an order clause to the query string.
   *
   * @param sb the buffer for the query string
   * @param collectionOptions the options to use for the order.
   */
  private void addOrderClause(StringBuilder sb, CollectionOptions collectionOptions) {
    String sortBy = collectionOptions.getSortBy();
    if (sortBy != null && sortBy.length() > 0) {
      if (ApplicationService.TOP_FRIENDS_SORT.equals(sortBy)) {
        // TODO sorting by friend.score doesn't work right now because of group by issue (see above TODO)
        // this assumes that the query is a join with the friends store.
        sb.append(" order by f.score ");
      } else {
        if ("name".equals(sortBy)) {
          // TODO Is this correct?
          // If sortBy is name then order by p.name.familyName, p.name.givenName.
          sb.append(" order by p.name.familyName, p.name.givenName ");
        } else {
          sb.append(" order by p.").append(sortBy);
        }
        switch (collectionOptions.getSortOrder()) {
        case ascending:
          sb.append(" asc ");
          break;
        case descending:
          sb.append(" desc ");
          break;
        }
      }
    }
  }
}
