<?php
use Agave\Bundle\ApiBundle\Auth\JWTClient;

class OperationsStatsController extends AbstractAgaveController {

  private $pingdom;

  /*
  |--------------------------------------------------------------------------
  | Operations Stats Controller
  |--------------------------------------------------------------------------
  |
  | Gives a general overview of ops stats
  |
  */

  protected function getPrimaryStats() {

    $this->pingdom = new PingdomClient($_ENV['pingdomUsername'], $_ENV['pingdomPassword'], $_ENV['pingdomToken'], $this->force);

    return array(
      "timeframe" => $this->getTimeframeString(),
      "totals" => $this->getTotals(),
      "averages" => $this->getAverages(),
      "leaders" => $this->getLeadersSeries(),
      // "locations" => $this->getLocations(),
      "_links" => $this->getHypermediaLinks()
    );
  }

  protected function getHypermediaLinks() {
    return array(
      'self' => array('href' => URL::action('OperationsStatsController@index')),
      'parent' => array('href' => URL::action('SummaryStatsController@index')),
    );
  }

  /**
   * Returns an object with aggregate totals for various user stats.
   */
  private function getTotals() {
    return array(
      "downtime" => $this->getCumulativeDowntime(),
      "maintenance" => 1,
      "deployments" => 5,
      "hosts" => 8,
      "datacenters" => 3,
    );
  }

  private function getCumulativeDowntime() {
    $downtime = 0;
    foreach ($this->pingdom->getChecks() as $check) {
      $clientDowntime = $this->pingdom->getDowntime($check->id);
      if ($clientDowntime) {
        $downtime += $clientDowntime;
      }
    }

    $hr = floor($downtime / (1000*60*60));
    if ($hr < 10) $hr = "0$hr";
    $min = floor(($downtime % (1000*60*60)) / (1000*60));
    if ($min < 10) $min = "0$min";
    $sec = floor((($downtime % (1000*60*60)) % (1000*60)) / 1000);
    if ($sec < 10) $sec = "0$sec";
    return "$hr:$min:$sec";
  }

  /**
   * Returns arrays of leaders in the operations categories.
   */
  private function getLeadersSeries() {
    return [
      array(
        "category" => "distribution",
        "values" => $this->getContainerUtilization()
      )
    ];
  }

  /**
   * Returns most used containers across the platform.
   */
  private function getContainerUtilization() {
    // query docker repo/orchestration for container info
    // File::get("https://index.docker.io/v1/search?q=agaveapi");
    // File::get("https://index.docker.io/v1/repositories/u/agaveapi");
    return array(
      array(
        "name" => "database",
        "count" => 6
      ),
      array(
        "name" => "worker",
        "count" => 4
      ),
      array(
        "name" => "api",
        "count" => 32
      ),
      array(
        "name" => "tenant",
        "count" => 96
      ),
      array(
        "name" => "website",
        "count" => 6
      ),
      array(
        "name" => "sandbox",
        "count" => (4 + rand(3,20))
      )
    );
  }

  private function getAverages() {

    $timestamp = 0;
    if (!empty($this->timeframe)) {
      $timestamp = strtotime("1 {$this->timeframe} ago");
    }

    $averages = array();
    $checks = $this->pingdom->getChecks();
    foreach ($checks as $check) {
      $averages[$check->name] = $this->pingdom->getUptime($check->id, $timestamp);
    }

    return $averages;
  }
}
