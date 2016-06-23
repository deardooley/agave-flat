<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Proxyservers
 */
class Proxyservers
{
    /**
     * @var integer
     */
    private $id;

    /**
     * @var string
     */
    private $host;

    /**
     * @var string
     */
    private $name;

    /**
     * @var integer
     */
    private $port;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Remoteconfigs
     */
    private $remoteConfig;


    /**
     * Get id
     *
     * @return integer 
     */
    public function getId()
    {
        return $this->id;
    }

    /**
     * Set host
     *
     * @param string $host
     * @return Proxyservers
     */
    public function setHost($host)
    {
        $this->host = $host;

        return $this;
    }

    /**
     * Get host
     *
     * @return string 
     */
    public function getHost()
    {
        return $this->host;
    }

    /**
     * Set name
     *
     * @param string $name
     * @return Proxyservers
     */
    public function setName($name)
    {
        $this->name = $name;

        return $this;
    }

    /**
     * Get name
     *
     * @return string 
     */
    public function getName()
    {
        return $this->name;
    }

    /**
     * Set port
     *
     * @param integer $port
     * @return Proxyservers
     */
    public function setPort($port)
    {
        $this->port = $port;

        return $this;
    }

    /**
     * Get port
     *
     * @return integer 
     */
    public function getPort()
    {
        return $this->port;
    }

    /**
     * Set remoteConfig
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Remoteconfigs $remoteConfig
     * @return Proxyservers
     */
    public function setRemoteConfig(\Agave\Bundle\ApiBundle\Entity\Remoteconfigs $remoteConfig = null)
    {
        $this->remoteConfig = $remoteConfig;

        return $this;
    }

    /**
     * Get remoteConfig
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Remoteconfigs 
     */
    public function getRemoteConfig()
    {
        return $this->remoteConfig;
    }
}
