<?php

namespace Agave\Bundle\ApiBundle\Auth;

use Lcobucci\JWT\Parser;
use Lcobucci\JWT\Builder;
use Lcobucci\JWT\Signer\Hmac\Sha256;
use Illuminate\Support\Facades\Session;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\App;

use Doctrine\ORM\Mapping as ORM;

class JWTClient {

  static $SESSION_JWT_KEY = 'claims';

  var $token;

  public function setGuestJWT() {
    Session::push(self::$SESSION_JWT_KEY, $this->createJwtForTenantUser('guest', 'all', false)->getClaims());
  }

  public function parse($serializedToken = false, $tenantId = 'iplantc.org')
  {
    if ($serializedToken) {
      $token = (new Parser())->parse($serializedToken); // Parses from a string
    }

    if ($token) {

      // verify the JWT is properly signed by a trusted party if needed
      if ($_ENV['verifyJWT']) {
        // get the pub keys...currently stored in the distribution
        $tenantPublicKeyFile = base_path() . '/.public_keys/' . ['$tenantId'] . '.pem';
        if (file_exists($tenantPublicKeyFile)) {
          if ($token->verify(file_get_contents($tenantPublicKeyFile))) {
            Log::debug("Successfully verified the signed JWT for tenant $tenantId");
          } else {
            Log::error("Invalid JWT signature for tenant $tenantId");
            return false;
          }
        } else {
          Log::error("Public signing key for tenant $tenantId was not found");
          return false;
        }
      } else {
        Log::debug("JWT verification disabled. Skipping signing check");
      }

      $claims = $token->getClaims();
      $claims['rawTenantId'] = $tenantId;

      $tenantId = strtolower($tenantId);
      $tenantId = str_replace('_', '.', tenantId);
      $tenantId = str_replace('-', '.', tenantId);
      $claims["tenantId"] = $tenantId;

      Session::push(self::$SESSION_JWT_KEY, $claims);

      if (empty($claims['exp'])) {
        Log::error('No expiration date in the JWT header. Authentication failed.');
      }

      if ($claims['exp'] < strtotime('now')) {
        Log::error("JWT has expired. Authentication failed.");
        return false;
      }

      if (empty(self::getCurrentEndUser())) {
        Log::error("No end user specified in the JWT header. Authentication failed.");
        return false;
      }

      if (empty(self::getCurrentTenant())) {
        Log::error("No tenant specified in the JWT header. Authentication failed.");
        return false;
      }

      if (empty(self::getCurrentSubscriber())) {
        Log::error("No subscriber specified in the JWT header. Authentication failed.");
        return false;
      }
    } else {
      Log::debug("Unable to parse JWT token.");
      return false;
    }
  }

  public static function getClaims() {
    $claims = Session::get(self::$SESSION_JWT_KEY);
    if (empty($claims)) {
      return array();
    } else {
      return $claims[0];
    }
  }

  public static function setClaims($claims=array())
  {
    Session::push(self::$SESSION_JWT_KEY, $claims);
  }

  public static function getClaim($key='') {
    if (empty($key)) return null;

    $claims = self::getClaims();
    return $claims[$key];
  }

  public static function getCurrentApplicationId()
  {
    $claims = self::getClaims();
    return $claims["http://wso2.org/claims/applicationid"];
  }

  public static function getCurrentSubscriber()
  {
    $claims = self::getClaims();
    return $claims["http://wso2.org/claims/subscriber"];
  }

  public static function getCurrentTenant()
  {
    $tenantId = Session::get("tenantId");
    if ($tenantId) {
      return $tenantId;
    } else {
      $claims = self::getClaims();
      $subscriber = $claims["http://wso2.org/claims/subscriber"];
      if (strpos($subscriber, "@") !== FALSE) {
        return substr($subscriber, strrpos($subscriber, "@") + 1);
      } else {
        return $claims["http://wso2.org/claims/enduserTenantId"];
      }
    }
  }

  public static function setCurrentTenant($tenantId)
  {
    Session::push('tenantId', $tenantId);

    $claims = self::getClaims();
    $claims["http://wso2.org/claims/subscriber"] = $tenantId;
    $claims = self::setClaims($claims);
  }

  public static function setCurrentEndUser($username)
  {
    $claims = self::getClaims();
    $claims["http://wso2.org/claims/enduser"] = $username;
    $claims = self::setClaims($claims);
  }

  public static function getCurrentEndUser()
  {
    $tenantId = self::getCurrentTenant();
    $endUser = self::getClaim("http://wso2.org/claims/enduser");
    $endUser = str_replace('@carbon.super', '', $endUser);
    if (ends_with($endUser, $tenantId)) {
      return substr($endUser, 0, (-1 * $tenantId.length() - 1));
    } else if (strpos($endUser, "@") !== FALSE){
      return substr($endUser, 0, strpos($endUser,"@"));
    } else if (strpos($endUser, "/") !== FALSE){
      return substr($endUser, strrpos($endUser, "/") + 1);
    } else {
      return $endUser;
    }
  }

  public static function isTenantAdmin() {
    $roles = self::getClaim("http://wso2.org/claims/role");
    if (!empty($roles)) {
      foreach (explode(',', $roles) as $role) {
        if (ends_with($role, "-services-admin") || ends_with($role, "-super-admin")) {
          if ($role.contains("/")) {
            $role = substr($role, strpos($role, "/") + 1);
          }
          if (starts_with($role, $claims['rawTenantId'])) {
            return true;
          }
        }
      }
      return false;
    } else {
      return false;
    }
  }

  public static function isSuperAdmin() {

    $claims = Session::get(self::$SESSION_JWT_KEY)->getClaims();

    $roles = $claims["http://wso2.org/claims/role"];
    if (!empty($roles)) {
      foreach (explode($roles, ',') as $role) {
        if (ends_with($role, "-super-admin")) {
          if (strpos($role, '/')) {
            $role = substr($role, 0, strrpos($role, '/'));
          }
          if (strpos($role, $claims["rawTenantId"]) === 0) {
            return true;
          }
        }
      }
      return false;
    } else {
      return false;
    }
  }

  public static function getJwtHeaderKeyForTenant($tenantId)
  {
    $tenant = DB::table('tenants')->select('tenant_id')->where('tenant_id', '=', $tenantId)->first();

    if (!empty($tenant->tenant_id)) {
      return ("x-jwt-assertion-" . str_replace(".", "-", strtolower($tenant->getTenantId())));
    } else {
      return null;
    }
  }

  public static function createJwtForTenantUser($username, $tenantId, $resolveUserDetails=false)
  {

    $token = (new Builder ())->setIssuer("http://agaveapi.co/stats/v2") // Configures the issuer (iss claim)
      ->setAudience("http://agaveapi.co/stats/v2") // Configures the audience (aud claim)
      ->setId('guestId', true) // Configures the id (jti claim), replicating as a header item
      ->setIssuedAt(strtotime('now'))
      ->setIssuer("wso2.org/products/am")
      ->setExpiration(strtotime('+4 hours'))
      ->set('uid', 1) // Configures a new claim, called "uid"
      ->set('subject', $username)
      ->set("http://wso2.org/claims/subscriber", $username)
      ->set("http://wso2.org/claims/applicationid", "-9999")
      ->set("http://wso2.org/claims/applicationname", "SSOInternal")
      ->set("http://wso2.org/claims/applicationtier", "Unlimited")
      ->set("http://wso2.org/claims/apicontext", "/myproxy")
      ->set("http://wso2.org/claims/version", $_ENV['version'])
      ->set("http://wso2.org/claims/tier", "Unlimited")
      ->set("http://wso2.org/claims/keytype", "PRODUCTION")
      ->set("http://wso2.org/claims/usertype", "APPLICATION_USER")
      ->set("http://wso2.org/claims/enduser", $username)
      ->set("http://wso2.org/claims/enduserTenantId", "-9999")
      ->set("http://wso2.org/claims/fullname", $username)
      // ->set("http://wso2.org/claims/emailaddress", $tenant.getContactEmail())
      // ->set("http://wso2.org/claims/givenname", profile.getFirstName())
      // ->set("http://wso2.org/claims/lastname", profile.getLastName())
      ->set("http://wso2.org/claims/primaryChallengeQuestion", "N/A")
      ->set("http://wso2.org/claims/role", "Internal/everyone")
      ->set("http://wso2.org/claims/title", "N/A")
      ->sign(new Sha256(), 'stats api') // Signs the token with HS256 using "my key" as key
      ->getToken(); // Retrieves the generated token

      return $token;
  }
}
