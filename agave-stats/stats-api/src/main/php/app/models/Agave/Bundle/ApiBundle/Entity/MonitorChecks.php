<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * MonitorChecks
 */
class MonitorChecks
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
    private $message;

    /**
     * @var string
     */
    private $result;

    /**
     * @var string
     */
    private $tenantId;

    /**
     * @var string
     */
    private $uuid;

    /**
     * @var string
     */
    private $type;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Monitors
     */
    private $monitor;


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
     * @return MonitorChecks
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
     * Set message
     *
     * @param string $message
     * @return MonitorChecks
     */
    public function setMessage($message)
    {
        $this->message = $message;

        return $this;
    }

    /**
     * Get message
     *
     * @return string 
     */
    public function getMessage()
    {
        return $this->message;
    }

    /**
     * Set result
     *
     * @param string $result
     * @return MonitorChecks
     */
    public function setResult($result)
    {
        $this->result = $result;

        return $this;
    }

    /**
     * Get result
     *
     * @return string 
     */
    public function getResult()
    {
        return $this->result;
    }

    /**
     * Set tenantId
     *
     * @param string $tenantId
     * @return MonitorChecks
     */
    public function setTenantId($tenantId)
    {
        $this->tenantId = $tenantId;

        return $this;
    }

    /**
     * Get tenantId
     *
     * @return string 
     */
    public function getTenantId()
    {
        return $this->tenantId;
    }

    /**
     * Set uuid
     *
     * @param string $uuid
     * @return MonitorChecks
     */
    public function setUuid($uuid)
    {
        $this->uuid = $uuid;

        return $this;
    }

    /**
     * Get uuid
     *
     * @return string 
     */
    public function getUuid()
    {
        return $this->uuid;
    }

    /**
     * Set type
     *
     * @param string $type
     * @return MonitorChecks
     */
    public function setType($type)
    {
        $this->type = $type;

        return $this;
    }

    /**
     * Get type
     *
     * @return string 
     */
    public function getType()
    {
        return $this->type;
    }

    /**
     * Set monitor
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Monitors $monitor
     * @return MonitorChecks
     */
    public function setMonitor(\Agave\Bundle\ApiBundle\Entity\Monitors $monitor = null)
    {
        $this->monitor = $monitor;

        return $this;
    }

    /**
     * Get monitor
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Monitors 
     */
    public function getMonitor()
    {
        return $this->monitor;
    }
}
