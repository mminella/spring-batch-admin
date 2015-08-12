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

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.RestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.admin.service.FileInfo;
import org.springframework.batch.admin.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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
		List<FileInfo> files = new ArrayList<>();
		files.add(new FileInfo("foo.txt", "sometimestamp", true, 0));
		files.add(new FileInfo("bar/foo.txt", "anothertimestamp", false, 0));
		files.add(new FileInfo("bar/baz.txt", "lasttimestamp", true, 0));

		when(fileService.getFiles(0, 10)).thenReturn(files);

		mockMvc.perform(
				get("/batch/files").param("page", "0").param("size", "10").accept(MediaType.APPLICATION_JSON)).andDo(document("file-list",
				queryParameters(parameterWithName("page").description("Requested page index (0 based)"),
						parameterWithName("size").description("Number of elements per page")),
				responseFields(fieldWithPath("pagedResources.page").description("<<overview-pagination-response>>"),
						fieldWithPath("pagedResources.content").description("Array of <<file-resource>>"),
						fieldWithPath("pagedResources.links").description("Links to the current page of <<file-resource>>"))));
	}

	@Test
 	public void testDelete() throws Exception {
		when(fileService.delete("*")).thenReturn(3);

		mockMvc.perform(
          delete("/batch/files/*").accept(MediaType.APPLICATION_JSON)).andDo(print()).andDo(document("file-delete",
				responseFields(fieldWithPath("fileInfoResource").description("<<file-resource>>")
				))).andExpect(status().isOk())
			 .andExpect(jsonPath("$.fileInfoResource.deleteCount", equalTo(3)));
	}

	@Ignore
	@Test
	public void testUploadRequest() throws Exception {
		File tempFile = File.createTempFile("result", "txt");

		FileInfo fileInfo = new FileInfo(tempFile.getPath());
		when(fileService.createFile("/foo/bar.txt")).thenReturn(fileInfo);
		when(fileService.getResource(tempFile.getPath())).thenReturn(new FileSystemResource(tempFile));

		MockMultipartFile file = new MockMultipartFile("file", "bar.txt", "text/plain", "bar".getBytes());

		mockMvc.perform(MockMvcRequestBuilders.fileUpload("/batch/files")
				.file(file).param("path", "/foo").accept(MediaType.APPLICATION_JSON))
				.andDo(print())
		.andDo(document("file-upload-request",
				requestFields(fieldWithPath("path").description("where the file should be uploaded to"))));

		verify(fileService).publish(fileInfo);
	}
}
