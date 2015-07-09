/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.admin.web;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.RestDocumentation.document;
import static org.springframework.restdocs.hypermedia.LinkExtractors.halLinks;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.admin.service.FileInfo;
import org.springframework.batch.admin.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * @author Michael Minella
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestDependencies.class, RestConfiguration.class})
@WebAppConfiguration
public class BatchFilesApiDocumentation extends AbstractApiDocumentation {

	@Autowired
	private FileService fileService;

	@Test
	public void testList() throws Exception {
		List<FileInfo> files = new ArrayList<FileInfo>();
		files.add(new FileInfo("foo.txt", "sometimestamp", true, 0));
		files.add(new FileInfo("bar/foo.txt", "anothertimestamp", false, 0));
		files.add(new FileInfo("bar/baz.txt", "lasttimestamp", true, 0));

		when(fileService.getFiles(0, 10)).thenReturn(files);

		mockMvc.perform(
				get("/batch/files").param("page", "0").param("size", "10").accept(MediaType.APPLICATION_JSON)).andDo(document("file-list")
				.withQueryParameters(parameterWithName("page").description("Requested page index (0 based)"),
						parameterWithName("size").description("Number of elements per page"))
				.withResponseFields(fieldWithPath("pagedResources.content").description("An array of <<file-info,FileInfo resources>>"),
						fieldWithPath("pagedResources.page.size").description("The requested page size"),
						fieldWithPath("pagedResources.page.totalElements").description("The total number of elements"),
						fieldWithPath("pagedResources.page.totalPages").description("The total number of pages"),
						fieldWithPath("pagedResources.page.number").description("The current page number"))
				.withLinks(halLinks()));
	}
}

/**
 {
 "pagedResources" : {
 "links" : [ {
 "rel" : "self",
 "href" : "http://localhost/batch/files{?page,size,sort}"
 } ],
 "content" : [ {
 "timestamp" : "sometimestamp",
 "path" : "foo.txt",
 "shortPath" : "foo.txt",
 "local" : true,
 "deleteCount" : 0,
 "links" : [ {
 "rel" : "self",
 "href" : "http://localhost/batch/files/foo.txt"
 } ]
 }, {
 "timestamp" : "anothertimestamp",
 "path" : "bar/foo.txt",
 "shortPath" : "bar/foo.txt",
 "local" : false,
 "deleteCount" : 0,
 "links" : [ {
 "rel" : "self",
 "href" : "http://localhost/batch/files/bar/foo.txt"
 } ]
 }, {
 "timestamp" : "lasttimestamp",
 "path" : "bar/baz.txt",
 "shortPath" : "bar/baz.txt",
 "local" : true,
 "deleteCount" : 0,
 "links" : [ {
 "rel" : "self",
 "href" : "http://localhost/batch/files/bar/baz.txt"
 } ]
 } ],
 "page" : {
 "size" : 10,
 "totalElements" : 0,
 "totalPages" : 0,
 "number" : 0
 }
 }
 }
 */