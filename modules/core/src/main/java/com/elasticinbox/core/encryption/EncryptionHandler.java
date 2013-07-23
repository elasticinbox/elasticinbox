/**
 * Copyright (c) 2011-2012 Optimax Software Ltd.
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

package com.elasticinbox.core.encryption;

import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

import com.elasticinbox.core.model.Message;

/**
 * Handles generic encryption/decryption tasks. Used mainly for dependency injection.
 * 
 * @author Rustam Aliyev
 */
public interface EncryptionHandler
{
	/**
	 * Encrypt input stream
	 *  
	 * @param in Clear input stream
	 * @param key AES key used for encryption
	 * @param iv Initialisation vector
	 * @return Encrypted input stream
	 * @throws GeneralSecurityException
	 */
	public InputStream encrypt(InputStream in, Key key, byte[] iv) throws GeneralSecurityException;

	/**
	 * Decrypt input stream
	 *  
	 * @param in Encrypted input stream
	 * @param key AES key used for encryption
	 * @param iv Initialisation vector used for encryption
	 * @return Decrypted input stream
	 * @throws GeneralSecurityException
	 */
	public InputStream decrypt(InputStream in, Key key, byte[] iv) throws GeneralSecurityException;

	/*
	 * encrypt a message object
	 */
	public Message encryptMessage(Message message,
			Key blobStoreDefaultEncryptionKey, byte[] iv) throws  NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, ShortBufferException, BadPaddingException, IllegalBlockSizeException, BadPaddingException;

	/*
	 * decrypt a message object
	 */
	public Message decryptMessage(Message message,
			Key blobStoreDefaultEncryptionKey, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, ShortBufferException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException;
}
