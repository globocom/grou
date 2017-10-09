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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import java.time.Instant;

public interface TestRepository extends MongoRepository<Test, String> {

    Page<Test> findByName(@Param("name") String name, Pageable pageable);

    Page<Test> findByStatus(@Param("status") Test.Status status, Pageable pageable);

    Page<Test> findByProject(@Param("name") String name, Pageable pageable);

    Page<Test> findByTags(@Param("key") String key, Pageable pageable);

    Page<Test> findByLoadersName(@Param("name") String name, Pageable pageable);

    Page<Test> findByCreatedBy(@Param("name") String name, Pageable pageable);

    Page<Test> findByLastModifiedBy(@Param("name") String name, Pageable pageable);

    @Query(value = "{'createdDate':{ $gte: ?0, $lte: ?1}}")
    Page<Test> findByCreatedDateBetween(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Override
    @RestResource(exported = false)
    void delete(Test test);
}
