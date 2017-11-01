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
import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.websocket.server.PathParam;
import java.time.Instant;

public interface TestRepository extends MongoRepository<Test, String> {


    @Override
    @ApiOperation(value="Requests a Test", notes = "A new test is created and sent to the queue")
    @ApiResponses(
            @ApiResponse(
                    code = 200,
                    message = "Test was created and sent to groot",
                    response = Test.class,
                    responseHeaders =
                    @ResponseHeader(
                            name="X-Auth-Token",
                            description="Keystone Token",
                            response=String.class
                    )
            )
    )
    Test save(Test test);

    Page<Test> findByName(@Param("name") String name, Pageable pageable);

    Page<Test> findByStatus(@Param("status") Test.Status status, Pageable pageable);

    Page<Test> findByProject(@Param("name") String name, Pageable pageable);

    Page<Test> findByProjectAndStatus(@Param("name") String name, @Param("status") Test.Status status, Pageable pageable);

    Page<Test> findByTags(@Param("key") String key, Pageable pageable);

    Page<Test> findByTagsAndProject(@Param("key") String key, @Param("project") String project, Pageable pageable);

    Page<Test> findByLoadersName(@Param("name") String name, Pageable pageable);

    Page<Test> findByCreatedBy(@Param("name") String name, Pageable pageable);

    Page<Test> findByLastModifiedBy(@Param("name") String name, Pageable pageable);

    @Query(value = "{'createdDate':{ $lte: ?0 }}")
    Page<Test> findByCreatedDateBefore(@Param("instante") Instant instant, Pageable pageable);

    @Query(value = "{'project': ?0, 'createdDate':{ $gte: ?1 }}")
    Page<Test> findByProjectAndCreatedDateAfter(@Param("project") String project, @Param("instante") Instant instant, Pageable pageable);

    @Query(value = "{'createdDate':{ $gte: ?0, $lte: ?1}}")
    Page<Test> findByCreatedDateBetween(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    Page<Test> findByNameAndProject(@Param("name") String name, @Param("project") String project, Pageable pageable);

    @Query(value = "{'lastModifiedDate':{ $lte: ?0}}")
    Page<Test> findByLastModifiedByBefore(@Param("instant") Instant instant, Pageable pageable);

    @Query(value = "{'lastModifiedDate':{ $gte: ?0}}")
    Page<Test> findByLastModifiedByAfter(@Param("instant") Instant instant, Pageable pageable);

    @Query(value = "{'lastModifiedDate':{ $lte: ?0 }, 'status': ?1 }")
    Page<Test> findByLastModifiedByBeforeAndStatus(@Param("instant") Instant instant, @Param("status") Test.Status status, Pageable pageable);

    @Query(value = "{'lastModifiedDate':{ $gte: ?0 }, 'status': ?1 }")
    Page<Test> findByLastModifiedByAfterAndStatus(@Param("instant") Instant instant, @Param("status") Test.Status status, Pageable pageable);

    @Override
    @RestResource(exported = false)
    void delete(Test test);
}
