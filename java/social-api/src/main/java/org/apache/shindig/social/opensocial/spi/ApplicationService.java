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
package org.apache.shindig.social.opensocial.spi;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.social.opensocial.model.Application;

import java.util.Set;
import java.util.concurrent.Future;

/**
 * Interface that defines how shindig gathers spaces information.
 */
public interface ApplicationService {

  /**
   * When used will sort people by the container's definition of top friends. Note that both the
   * sort order and the filter are required to deliver a topFriends response. The PersonService
   * implementation should take this into account when delivering a topFriends response.
   */
  public static String TOP_FRIENDS_SORT = "topFriends";
  /**
   * Retrieves only the user's top friends. The meaning of top and how many top is is defined by the
   * PersonService implementation.
   */
  public static String TOP_FRIENDS_FILTER = "topFriends";
  /**
   * Retrieves all friends with any data for this application.
   * TODO: how is this application defined
   */
  public static String HAS_APP_FILTER = "hasApp";
  /**
   * Retrieves all friends. (ie no filter)
   */
  public static String ALL_FILTER = "all";
  /**
   * Will filter the people requested by checking if they are friends with the given idSpec. The
   * filter value will be set to the userId of the target friend.
   */
  public static String IS_WITH_FRIENDS_FILTER = "isFriendsWith";

  /**
   * Returns a list of applications for the context.
   *
   * @param contexts A context for which applications to be returned
   * @param collectionOptions How to filter, sort and paginate the collection being fetched
   * @param fields The profile details to fetch. Empty set implies all
   * @param token The gadget token @return a list of people.
   * @return Future that returns a RestfulCollection of Application
   */
  Future<RestfulCollection<Application>> getApplicationsForContext(Context context,
      CollectionOptions collectionOptions, Set<String> fields, SecurityToken token)
      throws ProtocolException;
  
  /**
   * Returns a list of applications that correspond to the passed in applicationsIds.
   *
   * @param applicationsIds A set of application ids
   * @param collectionOptions How to filter, sort and paginate the collection being fetched
   * @param fields The profile details to fetch. Empty set implies all
   * @param token The gadget token @return a list of people.
   * @return Future that returns a RestfulCollection of Application
   */
  Future<RestfulCollection<Application>> getApplications(Set<ApplicationId> applicationIds,
      CollectionOptions collectionOptions, Set<String> fields, SecurityToken token)
      throws ProtocolException;
  
  /**
   * Returns an application that corresponds to the passed in application id.
   *
   * @param id The application id for which application info to be fetched.
   * @param fields The fields to fetch.
   * @param token The gadget token
   * @return an application.
   */
  Future<Application> getApplication(ApplicationId applicationId, Set<String> fields, SecurityToken token)
      throws ProtocolException;
	  
	  
  /**
   * Create a gadget in current widgetspace that correspons to the passed in
   * application url
   * 
   * @param url
   *          The url of the source file for the application to be created
   * @param token
   *          The gadget token
   */
  Future<Void> createApplication(ApplicationUrl applicationUrl, SecurityToken token) throws ProtocolException;
}
