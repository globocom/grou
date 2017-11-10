/*
 * Copyright (c) 2017-2017 Globo.com
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.globocom.grou.entities.repositories;
import com.globocom.grou.entities.Test;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import java.time.Instant;

public interface TestRepository extends MongoRepository<Test, String> {

    @Override
    @ApiOperation(value="Request a Test", notes = "A new test is created and sent to the queue. Header with Keystone token is needed in order to perform this action." +
            "Parameters name, project and properties (previously set on Keystone) are required.",
            httpMethod = "POST"
    )
    @ApiResponses(
            @ApiResponse(
                    code = 200,
                    message = "Test was created and sent to load generator",
                    response = Test.class
            )
    )
    Test save(Test test);


    @ApiOperation(value="Get all tests", notes = "Returns all tests. No token is needed")
    Page<Test> findAll(Pageable pageable);

    @ApiOperation(value="Get one test", notes = "Returns a test by its ID. No token is needed")
    Test findOne(@Param("id") String id);

    @ApiOperation(value="Find tests by Name", notes = "Returns tests with matching name. No token is needed")
    Page<Test> findByName(@Param("name") String name, Pageable pageable);

    @ApiOperation(value="Find tests by Status", notes = "Returns tests with matching status. The possible statuses are SCHEDULED, ENQUEUED, RUNNUNG, ABORTED, OK and ERROR. No token is needed.")
    Page<Test> findByStatus(@Param("status") Test.Status status, Pageable pageable);

    @ApiOperation(value="Find tests by Project", notes = "Returns tests with matching project. No token is needed.")
    Page<Test> findByProject(@Param("name") String name, Pageable pageable);

    @ApiOperation(value="Find tests by Project and Status", notes = "Returns tests with matching project and status. No token is needed.")
    Page<Test> findByProjectAndStatus(@Param("name") String name, @Param("status") Test.Status status, Pageable pageable);

    @ApiOperation(value="Find tests by Tags", notes = "Returns tests with matching tags. No token is needed.")
    Page<Test> findByTags(@Param("key") String key, Pageable pageable);

    @ApiOperation(value="Find tests by Tags and Projects", notes = "Returns tests with matching tag. No token is needed.")
    Page<Test> findByTagsAndProject(@Param("key") String key, @Param("project") String project, Pageable pageable);

    @ApiOperation(value="Find tests by LoadersName", notes = "Returns tests with matching loader. No token is needed.")
    Page<Test> findByLoadersName(@Param("name") String name, Pageable pageable);

    @ApiOperation(value="Find tests by Created date", notes = "Returns tests with matching creation date. No token is needed.")
    Page<Test> findByCreatedBy(@Param("name") String name, Pageable pageable);

    @ApiOperation(value="Find test by LastModified date", notes = "Returns tests with matching last modification date. No token is needed.")
    Page<Test> findByLastModifiedBy(@Param("name") String name, Pageable pageable);

    @ApiOperation(value="Find tests by CreatedDateBefore", notes = "Returns tests created before given date. No token is needed.")
    @Query(value = "{'createdDate':{ $lte: ?0 }}")
    Page<Test> findByCreatedDateBefore(@Param("instante") Instant instant, Pageable pageable);

    @ApiOperation(value="Find tests by Project and CreatedDateAfter", notes = "Returns tests with matching project and created after given date. No token is needed.")
    @Query(value = "{'project': ?0, 'createdDate':{ $gte: ?1 }}")
    Page<Test> findByProjectAndCreatedDateAfter(@Param("project") String project, @Param("instante") Instant instant, Pageable pageable);

    @ApiOperation(value="Find tests by CreatedDateBetween", notes = "Returns tests matching interval of dates. No token is needed.")
    @Query(value = "{'createdDate':{ $gte: ?0, $lte: ?1}}")
    Page<Test> findByCreatedDateBetween(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @ApiOperation(value="Find tests by Name and Project", notes = "Returns tests with matching name and project. No token is needed.")
    Page<Test> findByNameAndProject(@Param("name") String name, @Param("project") String project, Pageable pageable);

    @ApiOperation(value="Find tests by LastModifiedDate", notes = "Returns tests with last modified date before given date. No token is needed.")
    @Query(value = "{'lastModifiedDate':{ $lte: ?0}}")
    Page<Test> findByLastModifiedByBefore(@Param("instant") Instant instant, Pageable pageable);

    @ApiOperation(value="Find tests by LastModifiedAfter", notes = "Returns tests with last modified date after given date. No token is needed.")
    @Query(value = "{'lastModifiedDate':{ $gte: ?0}}")
    Page<Test> findByLastModifiedByAfter(@Param("instant") Instant instant, Pageable pageable);

    @ApiOperation(value="Find tests by LastModifiedByBefore date and Status", notes = "Returns tests with matching status and last modified date before given date. No token is needed.")
    @Query(value = "{'lastModifiedDate':{ $lte: ?0 }, 'status': ?1 }")
    Page<Test> findByLastModifiedByBeforeAndStatus(@Param("instant") Instant instant, @Param("status") Test.Status status, Pageable pageable);

    @ApiOperation(value="Find tests by LastModifiedByAfter and Status", notes = "Returns tests with matching status and last modified date after given date. No token is needed.")
    @Query(value = "{'lastModifiedDate':{ $gte: ?0 }, 'status': ?1 }")
    Page<Test> findByLastModifiedByAfterAndStatus(@Param("instant") Instant instant, @Param("status") Test.Status status, Pageable pageable);

    @Override
    @RestResource(exported = false)
    @ApiOperation(value = "", hidden = true)
    void delete(Test test);

}
