<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Userdefaultsystems
 */
class Userdefaultsystems
{
    /**
     * @var string
     */
    private $username;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Systems
     */
    private $system;


    /**
     * Set username
     *
     * @param string $username
     * @return Userdefaultsystems
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
     * Set system
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Systems $system
     * @return Userdefaultsystems
     */
    public function setSystem(\Agave\Bundle\ApiBundle\Entity\Systems $system = null)
    {
        $this->system = $system;

        return $this;
    }

    /**
     * Get system
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Systems 
     */
    public function getSystem()
    {
        return $this->system;
    }
}
