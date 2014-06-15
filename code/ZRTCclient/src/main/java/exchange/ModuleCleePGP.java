/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package exchange;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.zeromq.*;
import org.zeromq.ZMQ.*;

/**
 *
 * @author Gauthier
 */
public class ModuleCleePGP {
    private static ZMQ.Context context;
    
    public ModuleCleePGP(ZMQ.Context context){
        this.context = context;
    }
    
    public static void test() throws Exception{
        try
		{
			// Génération de la paire de clé privée/publique à utiliser
			AsymmetricCipherKeyPairGenerator gen = new RSAKeyPairGenerator();
			
			gen.init(new RSAKeyGenerationParameters(
					new BigInteger("10001", 16),//publicExponent
			        SecureRandom.getInstance("SHA1PRNG"),//prng
			        1024,//strength
			        80//certainty
			        ));
			AsymmetricCipherKeyPair kPair = gen.generateKeyPair();
			
			BcPGPKeyPair keyPair = new BcPGPKeyPair(PublicKeyAlgorithmTags.RSA_SIGN, kPair, new Date());
			
			// On récupère la clé privée
			PGPPrivateKey prKey = keyPair.getPrivateKey();
			
			// On récupère la clé publique
			PGPPublicKey pKey = keyPair.getPublicKey();
			
			// On récupère la publique sous une forme transportable
			byte[] encoded =  pKey.getEncoded();
			
			// Conversion en B64 (à voir si à utiliser à ce niveau là ou automatiquement
			String output = DatatypeConverter.printBase64Binary(encoded);
			
			// On créé le générateur de signature
			PGPSignatureGenerator sigGen = new PGPSignatureGenerator(new BcPGPContentSignerBuilder(pKey.getAlgorithm(), PGPUtil.SHA256));
			
			// On initialise avec la clé privé
			sigGen.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, prKey);
			// On update avec le texte à signer
			sigGen.update("ceci est un test de signature".getBytes());
			// On génère la signature
			PGPSignature signature = sigGen.generate();
			// On la récupère soous une forme transportable et on l'encode en B64
			String encSig = DatatypeConverter.printBase64Binary(signature.getEncoded());
			
			System.out.println(output);
			
                        
                        
			
			// NE TE CONCERNE PAS CAR C'EST POUR LA VERIF DE SIGNATURE
			
			PGPPublicKeyRing ring = new PGPPublicKeyRing(DatatypeConverter.parseBase64Binary(output), new JcaKeyFingerprintCalculator());
			
			PGPPublicKey recup = ring.getPublicKey();
			
			byte[] encoded2 =  recup.getEncoded();
			
			String output2 = DatatypeConverter.printBase64Binary(encoded2);
			
			System.out.println(output2);
			
			PGPObjectFactory fact = new PGPObjectFactory(DatatypeConverter.parseBase64Binary(encSig));
			PGPSignatureList sigList = (PGPSignatureList)fact.nextObject();
			PGPSignature decSig = sigList.get(0);
			
			decSig.init(new BcPGPContentVerifierBuilderProvider(), recup);
			decSig.update("ceci est un test de signature".getBytes());
			System.out.println(decSig.verify());
			
		}
		catch(Exception ex)
		{
			System.out.println(ex);
		}
    }
}
