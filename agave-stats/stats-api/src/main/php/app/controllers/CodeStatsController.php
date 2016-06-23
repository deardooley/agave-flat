<?php
use Agave\Bundle\ApiBundle\Auth\JWTClient;

class CodeStatsController extends AbstractAgaveController {

  /*
  |--------------------------------------------------------------------------
  | Code Stats Controller
  |--------------------------------------------------------------------------
  |
  | Gives a general overview of stats from each code area
  |
  */

  private $bitbucket;

  // public function index()
  // {
  //   $stats = $this->resourceStats();
  //   $stats['_links'] = array(
  //     'self' => array('href' => URL::action('CodeStatsController@index')),
  //     'parent' => array('href' => URL::action('SummaryStatsController@index')),
  //   );
  //
  //   return Response::json(array(
  //     'status' => 'success',
  //     'message' => null,
  //     'errors' => [],
  //     'version' => $_ENV['version'],
  //     'result' => $stats
  //   ));
  // }
  //
  // private function resourceStats() {
  //   return array(
  //     "timeframe" => "month",
  //     "totals" => array(
  //       "openIssues" => 21,
  //       "releases" => 16,
  //       "featureRequests" => 6,
  //       "status" => 'up'));
  // }

  protected function getPrimaryStats() {

    $this->bitbucket = new BitbucketClient($this->force);

    return array(
      "timeframe" => $this->getTimeframeString(),
      "totals" => $this->getTotals(),
      "_links" => $this->getHypermediaLinks()
    );
  }

  protected function getHypermediaLinks() {
    return array(
      'self' => array('href' => URL::action('CodeStatsController@index')),
      'parent' => array('href' => URL::action('SummaryStatsController@index')),
    );
  }

  /**
   * Returns an object with aggregate totals for various user stats.
   */
  private function getTotals() {

    return array(
      "openIssues" => $this->getOpenIssueCount(),
      "releases" => $this->getCommitCount(),
      "featureRequests" => $this->getFeatureRequestCount(),
      "buildStatus" => $this->getBuildStatus()
    );
  }

  private function getOpenIssueCount() {
    return count($this->bitbucket->getOpenIssues());
    // return $this->bitbucket->getOpenIssues();
  }

  private function getCommitCount() {
    return $this->bitbucket->getCommitCount();
  }

  private function getFeatureRequestCount() {
    return count($this->bitbucket->getFeatureRequests());
  }

  private function getBuildStatus() {
    return 'UP';
  }

}
