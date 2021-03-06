/**
 * Copyright (c) 2011-2013 Optimax Software Ltd.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  * Neither the name of Optimax Software, ElasticInbox, nor the names
 *    of its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.elasticinbox.core.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class LabelMapTest
{
	@Test
	public void testIncrementCounters()
	{
		LabelMap labels = new LabelMap();
		
		LabelCounters lc = new LabelCounters();
		lc.setTotalMessages(120L);
		lc.setTotalBytes(1024000L);
		lc.setUnreadMessages(32L);
		
		LabelCounters diff = new LabelCounters();
		diff.setTotalMessages(19L);
		diff.setTotalBytes(24000L);
		diff.setUnreadMessages(5L);
		
		// increment initialized label
		labels.put(ReservedLabels.INBOX);

		int labelId = ReservedLabels.INBOX.getId();
		labels.get(labelId).setCounters(lc);
		labels.get(labelId).incrementCounters(diff);

		assertEquals(labels.get(labelId).getCounters().getTotalMessages().intValue(), 120+19);
		assertEquals(labels.get(labelId).getCounters().getTotalBytes().intValue(), 1024000+24000);
		assertEquals(labels.get(labelId).getCounters().getUnreadMessages().intValue(), 32+5);
	}

}
