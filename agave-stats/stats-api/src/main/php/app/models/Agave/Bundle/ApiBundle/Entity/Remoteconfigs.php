<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Remoteconfigs
 */
class Remoteconfigs
{
    /**
     * @var integer
     */
    private $id;

    /**
     * @var \DateTime
     */
    private $created;

    /**
     * @var string
     */
    private $host;

    /**
     * @var \DateTime
     */
    private $lastUpdated;

    /**
     * @var integer
     */
    private $port;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Proxyservers
     */
    private $proxyServer;


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
     * Set created
     *
     * @param \DateTime $created
     * @return Remoteconfigs
     */
    public function setCreated($created)
    {
        $this->created = $created;

        return $this;
    }

    /**
     * Get created
     *
     * @return \DateTime 
     */
    public function getCreated()
    {
        return $this->created;
    }

    /**
     * Set host
     *
     * @param string $host
     * @return Remoteconfigs
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
     * Set lastUpdated
     *
     * @param \DateTime $lastUpdated
     * @return Remoteconfigs
     */
    public function setLastUpdated($lastUpdated)
    {
        $this->lastUpdated = $lastUpdated;

        return $this;
    }

    /**
     * Get lastUpdated
     *
     * @return \DateTime 
     */
    public function getLastUpdated()
    {
        return $this->lastUpdated;
    }

    /**
     * Set port
     *
     * @param integer $port
     * @return Remoteconfigs
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
     * Set proxyServer
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Proxyservers $proxyServer
     * @return Remoteconfigs
     */
    public function setProxyServer(\Agave\Bundle\ApiBundle\Entity\Proxyservers $proxyServer = null)
    {
        $this->proxyServer = $proxyServer;

        return $this;
    }

    /**
     * Get proxyServer
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Proxyservers 
     */
    public function getProxyServer()
    {
        return $this->proxyServer;
    }
}
