<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Loginconfigs
 */
class Loginconfigs
{
    /**
     * @var string
     */
    private $protocol;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Remoteconfigs
     */
    private $id;


    /**
     * Set protocol
     *
     * @param string $protocol
     * @return Loginconfigs
     */
    public function setProtocol($protocol)
    {
        $this->protocol = $protocol;

        return $this;
    }

    /**
     * Get protocol
     *
     * @return string 
     */
    public function getProtocol()
    {
        return $this->protocol;
    }

    /**
     * Set id
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Remoteconfigs $id
     * @return Loginconfigs
     */
    public function setId(\Agave\Bundle\ApiBundle\Entity\Remoteconfigs $id = null)
    {
        $this->id = $id;

        return $this;
    }

    /**
     * Get id
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Remoteconfigs 
     */
    public function getId()
    {
        return $this->id;
    }
}
