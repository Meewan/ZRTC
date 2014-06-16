package exchange;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Gauthier
 */
public class User {
    private final String msgDelimiter = "#";
    private String nickname;
    private String password;
    private String fonction;
    private BcPGPKeyPair keyPair;
    private int keyAlgorithm;
    private PGPPrivateKey privateKey;
    
    public User(String nick, String passw, String fonct) throws NoSuchAlgorithmException, PGPException{
        this.nickname=nick;
        this.password=passw;
        this.fonction=fonct;
        
        // Initialisation de la paire de clés PGP
        AsymmetricCipherKeyPairGenerator gen = new RSAKeyPairGenerator();
		
		gen.init(new RSAKeyGenerationParameters(
				new BigInteger("10001", 16),//publicExponent
		        SecureRandom.getInstance("SHA1PRNG"),//prng
		        1024,//strength
		        80//certainty
		        ));
		AsymmetricCipherKeyPair kPair = gen.generateKeyPair();
		
		keyPair = new BcPGPKeyPair(PublicKeyAlgorithmTags.RSA_SIGN, kPair, new Date());
		privateKey = keyPair.getPrivateKey();
		keyAlgorithm = keyPair.getPublicKey().getAlgorithm();
    }
    
    public String getNick(){
        return this.nickname;
    }
    
    public void setNick(String nickname){
        this.nickname=nickname;
    }
    
    public void setPass(String password){
        this.password=password;
    }
    
    public String addSignature(String message) throws PGPException, SignatureException, IOException
    {
    	message = (message.endsWith("#"))?message.substring(0, message.length() - 1):message;
    	PGPSignatureGenerator sigGen = new PGPSignatureGenerator(new BcPGPContentSignerBuilder(keyAlgorithm, PGPUtil.SHA256));
		
		sigGen.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, privateKey);
		sigGen.update(message.getBytes());
		PGPSignature signature = sigGen.generate();
		System.out.println(message);
		return message+msgDelimiter+DatatypeConverter.printBase64Binary(signature.getEncoded());
    }
    
    public byte[] getPublicKey() throws IOException
    {
    	return keyPair.getPublicKey().getEncoded();
    }
}