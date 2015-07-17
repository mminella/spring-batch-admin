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
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
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
public class BatchJobsApiDocumentation extends AbstractApiDocumentation {

	private JobExecution execution;

	private TimeZone timeZone = TimeZone.getTimeZone("UTC");

	@Before
	public void before() throws Exception {
		Date startTime = new Date(1000);
		Date endTime = new Date(2000);
		execution = new JobExecution(0L,
				new JobParametersBuilder().addString("foo", "bar").addLong("foo2", 0L).toJobParameters());
		execution.setExitStatus(ExitStatus.COMPLETED);
		execution.setStartTime(startTime);
		execution.setEndTime(endTime);
		execution.setLastUpdated(new Date());
	}

	@Test
	public void testGetPageOfJobs() throws Exception {
		when(jobService.countJobs()).thenReturn(2);
		when(jobService.listJobs(0, 10)).thenReturn(Arrays.asList("job1", "job2"));
		when(jobService.isLaunchable("job1")).thenReturn(false);
		when(jobService.isLaunchable("job2")).thenReturn(true);
		when(jobService.countJobExecutionsForJob("job1")).thenReturn(2);
		when(jobService.countJobExecutionsForJob("job2")).thenReturn(1);
		when(jobService.isIncrementable("job1")).thenReturn(false);
		when(jobService.isIncrementable("job2")).thenReturn(true);
		when(jobService.listJobExecutionsForJob("job1", 0, 1)).thenReturn(Collections.singletonList(execution));

		mockMvc.perform(
				get("/batch/configurations").param("page", "0").param("size", "10")
				.accept(MediaType.APPLICATION_JSON)).andDo(print()).andDo(document("jobs")
				.withQueryParameters(parameterWithName("page").description("Requested page index (0 based)"),
						parameterWithName("size").description("Number of elements per page"))
				.withResponseFields(fieldWithPath("pagedResources.page").description("<<overview-pagination-response>>"),
						fieldWithPath("pagedResources.content").description("Array of <<job-detail-resource>>"),
						fieldWithPath("pagedResources.links").description("Links to the current page of <<job-detail-resource>>")));
	}

	@Ignore("path parameters are not supported yet.  See https://github.com/spring-projects/spring-restdocs/issues/86")
	@Test
	public void testJobInfo() throws Exception {
		when(jobService.isLaunchable("job1")).thenReturn(false);
		when(jobService.countJobExecutionsForJob("job1")).thenReturn(2);
		when(jobService.isIncrementable("job1")).thenReturn(false);
		JobExecution jobExecution = new JobExecution(5l);
		jobExecution.setLastUpdated(new Date());
		when(jobService.listJobExecutionsForJob("job1", 0, 1)).thenReturn(Collections.singletonList(jobExecution));

		mockMvc.perform(
				get("/batch/configurations/job1")
						.param("startJobInstance", "0")
						.param("pageSize", "20")
						.accept(MediaType.APPLICATION_JSON)).andDo(print()).andDo(document("job-detail-by-job"));
	}
}
