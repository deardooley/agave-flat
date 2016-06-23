package org.iplantc.service.notification.providers.realtime.clients;

import org.bouncycastle.util.encoders.Base64;
import org.iplantc.service.notification.Settings;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;

public class SignedFanoutJWT {

    private static JWSObject jwsObject;

    public static String getInstance() throws JOSEException {

        ObjectMapper mapper = new ObjectMapper();

        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        header.setType(new JOSEObjectType("JWT"));

        ObjectNode jwtBody = mapper.createObjectNode()
                .put("exp", (int) (new DateTime().plusSeconds(3600).getMillis() / 1000))
                .put("iss", Settings.REALTIME_REALM_ID);

        // Create an HMAC-protected JWS object with some payload
        jwsObject = new JWSObject(header, new Payload(jwtBody.toString()));

        // We need a 256-bit key for HS256 which must be pre-shared
        byte[] sharedKey = Base64.decode(Settings.REALTIME_REALM_KEY.getBytes());

        // Apply the HMAC to the JWS object
        jwsObject.sign(new MACSigner(sharedKey));

        return jwsObject.serialize();
    }
}