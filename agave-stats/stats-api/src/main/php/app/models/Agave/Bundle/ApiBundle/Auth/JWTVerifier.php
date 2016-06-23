<?php

namespace Agave\Bundle\ApiBundle\Auth;

use Lcobucci\JWT\Parser;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\App;

class JWTVerifier {

  static $JWT_HEADER_NAME = "x-jwt-assertion";
  static $INTERNALUSER_HEADER_NAME = "x-agave-internaluser";

  public static function verify(\Illuminate\Http\Request $request) {

    $jwtClient = new JWTClient();

    $tenantIds = Cache::remember('tenantIds', 60, function() {
      return DB::table("tenants")->where('status', '=', 'LIVE')->lists('tenant_id');
    });

    $foundValidHeader = false;

    // check for a HTTP header for a valid tenant
    foreach ($tenantIds as $tenantId) {

      $tenantHeader = self::$JWT_HEADER_NAME . '-' . str_replace('.','-', $tenantId);

      if ($request->has($tenantHeader)) {

        $foundValidHeader = true;

        $serializedJWT = $request->header($tenantHeader);

        if ($request->query('debugJWT')) {
          Log::debug($tenantHeader .' : ' . $serializedJWT);
        }

        // parse the JWT and validate signing/contents
        if ($jwtClient->parse($serializedJWT, $tenantId)) {

          // if an internal user id is present, record that
          if ($request->has(self::$INTERNALUSER_HEADER_NAME)) {

            $internalUsername = $request->header(self::$INTERNALUSER_HEADER_NAME);

            if (!empty($internalUsername)) {
              Session::push('internalUsername', $internalUsername);
              Log::debug("Found header for internal user $internalUsername. Adding to the request");
            } else {
              // Log::error("Empty internal user header found in the request.");
              App::error("400", "Empty internal user header found in the request.");
            }
          } else {
            Log::debug("JWT verified successfully.");
          }
        } else {
          // Log::error("Failed to parse the JWT. Verification failed");
          App::error("500", "Failed to parse the JWT. Verification failed");
        }

        break;

      } else {
        Log::debug("No $tenantHeader header found for tenant $tenantId");
      }
    }

    if (!$foundValidHeader) {
      error_log("Unauthenticated request. Setting guest jwt");
      $jwtClient->setGuestJWT();
      $foundValidHeader = true;
    }
    // echo '<pre>'.print_r(JWTClient::getClaims(),1).'</pre>';

    return $foundValidHeader;
  }

}
