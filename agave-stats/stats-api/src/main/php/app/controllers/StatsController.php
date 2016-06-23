<?php

class StatsController extends BaseController {

  /*
   ***************************************************************************
   * Stats Controller
   ***************************************************************************
   *
   * You may wish to use controllers instead of, or in addition to, Closure
   * based routes. That's great! Here is an example controller method to
   * get you started. To route to this controller, just add the route:
   *
   * @SWG\Get(
   *     path="/",
   *     summary="Summary endpoint for all stats resources",
   *     tags={"stats"},
   *     description="Muliple tags can be provided with comma seperated strings. Use tag1, tag2, tag3 for testing.",
   *     operationId="listStatsResources",
   *     consumes={"application/json"},
   *     produces={"application/json"},
   *     @SWG\Response(
   *         response=200,
   *         description="successful operation",
   *         @SWG\Schema(
   *             type="array",
   *             @SWG\Items(ref="#/definitions/Pet")
   *         ),
   *     ),
   *     security={
   *         {
   *             "oauth2": {"PRODUCTION"}
   *         }
   *     }
   * )
   */

  public function index()
  {
    return Response::json(array(
      'status' => 'success',
      'message' => null,
      'errors' => [],
      'version' => $_ENV['version'],
      'result' => array(
        '_links' => array(
          'self' => array('href' => URL::action('StatsController@index')),
          'summary' => array('href' => URL::action('SummaryStatsController@index')),
          'traffic' => array('href' => URL::action('TrafficStatsController@index')),
          'jobs' => array('href' => URL::action('JobStatsController@index')),
          'data' => array('href' => URL::action('DataStatsController@index')),
          'apps' => array('href' => URL::action('AppStatsController@index')),
          'code' => array('href' => URL::action('CodeStatsController@index')),
          'clients' => array('href' => URL::action('ClientStatsController@index')),
          'operations' => array('href' => URL::action('OperationsStatsController@index'))
        )
      )
    ));
  }

}
