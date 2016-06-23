<?php

namespace Agave\Bundle\ApiBundle\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\Controller;

class DefaultController extends Controller
{
    public function indexAction($name)
    {
        return $this->render('AgaveApiBundle:Default:index.html.twig', array('name' => $name));
    }
}
