<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Usagedeveloper
 */
class Usagedeveloper
{
    /**
     * @var string
     */
    private $username;

    /**
     * @var string
     */
    private $servicekey;


    /**
     * Set username
     *
     * @param string $username
     * @return Usagedeveloper
     */
    public function setUsername($username)
    {
        $this->username = $username;

        return $this;
    }

    /**
     * Get username
     *
     * @return string 
     */
    public function getUsername()
    {
        return $this->username;
    }

    /**
     * Set servicekey
     *
     * @param string $servicekey
     * @return Usagedeveloper
     */
    public function setServicekey($servicekey)
    {
        $this->servicekey = $servicekey;

        return $this;
    }

    /**
     * Get servicekey
     *
     * @return string 
     */
    public function getServicekey()
    {
        return $this->servicekey;
    }
}
