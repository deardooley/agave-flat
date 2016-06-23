<?php

use Agave\Bundle\ApiBundle\Auth\JWTClient;

abstract class AbstractAgaveController extends BaseController {

  /*
  |--------------------------------------------------------------------------
  | Summary Stats Controller
  |--------------------------------------------------------------------------
  |
  | Gives a general overview of stats from each major area
  |
  */

  protected $from;
  protected $to;
  protected $timeframe;
  protected $force;
  protected $apim;

  /**
   * Default constructor for all stat controllers in this API. It will parse the
   * URL parameters into class attributes and validate them. Reasonable defaults
   * are set if not present
   * @param DoctrineORMEntityManagerInterface $entityManager DI entity manager
   */
  public function __construct() {

    $this->from = strtotime(Request::query('from', '10 years') . ' ago');
    $this->to = strtotime(Request::query('to', 'now'));
    $this->force = strtolower(Request::query('force', 'false')) === 'true';
    $this->prettyPrint = strtolower(Request::query('pretty', 'false')) === 'true';
    $this->apim = new APIMClient($this->force);

//    $this->timeframe = Request::query('timeframe', '');
//    if (!empty($this->timeframe)) {
//      if (in_array(strtolower($this->timeframe), array('day', 'month', 'year'))) {
//        $this->timeframe = strtotime('1 ' .$this->timeframe . ' ago');
//      } else {
//        App::error("400", "Invalid timeframe value. Please specify one of day, month, or year");
//      }
//    }
  }

  public function index()
  {
    $body = array(
        'status' => 'success',
        'version' => $_ENV['version'],
        'result' => $this->getPrimaryStats()
    );
    return Response::json(
              $body,
              200,
              array(),
              $this->prettyPrint ? JSON_PRETTY_PRINT : null)
        ->header('Cache-Control', 'no-store, no-cache, must-revalidate, post-check=0, pre-check=0')
        ->header('Pragma', 'no-cache');

  }

  protected function getTimeframeString() {
    return empty($this->timeframe) ? 'all' : strtolower($this->timeframe);
  }

  /**
   * Returns the primary stats data for the given controller. This does not
   * include hyperlinked data.
   */
  abstract protected function getPrimaryStats();

  abstract protected function getHypermediaLinks();
  /**
   * Returns prefix used when caching db queries.
   */
  protected function getCachePrefix() {
    return $this->getControllerShortName() .'.'.JWTClient::getCurrentTenant();
  }

  /**
   * Returns short name of this controller. The short name is the lower case
   * controller name before 'StatsController'
   */
  protected function getControllerShortName() {
    $reflectionClass = new ReflectionClass($this);
    $name = $reflectionClass->getShortName();
    $name = str_replace('StatsController', '', $name);
    return strtolower($name);
  }

  /**
   * Adds username, tenant, and timeframe to query if not already present
   *
   * @param string $query the raw sql/dql query before filtering
   */
  protected function applyQueryConditions($query, $entityType) {
    $where = array();
    $username = JWTClient::getCurrentEndUser();
    $tenantId = JWTClient::getCurrentTenant();

    if ($username !== 'guest') {
      $where[] = sprintf(" r.%s = '%s' and r.tenantId = '%s'",
      $this->getOwnerFieldForEntity($entityType),
      $username,
      $tenantId);
    }

    if (!empty($this->timeframe) && $this->timeframe !== 'all') {
      $where[] = sprintf(" r.%s > '%s 00:00:00'",
      $this->getStartDateFieldForEntity($entityType),
      date('yyyy-mm-dd ', $this->timeframe));
    }

    if (!empty($where)) {
      if (strpos($query, ' where ') !== FALSE) {
        $query .= implode(' and ', $where);
      } else {
        $query .= ' where ' . implode(' and ', $where);
      }
    }
    // echo $query;
    return $query;
  }

  /**
   * Returns name of owner field in the Doctrine object. Since this
   * varies from entity to entity, we must resolve this in each controller.
   */
   protected function getOwnerFieldForEntity($entityType) {
     if ($entityType === 'usage') {
       return 'username';
     } else {
       return 'owner';
     }
   }

   /**
  * Returns name of the start date field in the Doctrine object. Since this
  * varies from entity to entity, we must resolve this in each controller.
  */
  protected function getStartDateFieldForEntity($entityType) {
    if ($entityType === 'usage') {
      return 'createdat';
    } else {
      return 'startTime';
    }
  }

  /**
   * Increments the $key value in the referenced array by $amount or creates
   * and sets the value if it does not exist
   * @param array  $array  the target array passed by reference
   * @param mixed  $key    the key to increment or create
   * @param integer $amount the amount to increment, defaults to 1
   */
  private function incrementOrCreate(&$array, $key, $amount=1) {
    if (empty($array[$key])) {
      $array[$key] = $amount;
    } else {
      $array[$key] += $amount;
    }
  }

}
