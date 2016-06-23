<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Usageactivities
 */
class Usageactivities
{
    /**
     * @var integer
     */
    private $id;

    /**
     * @var string
     */
    private $activitykey;

    /**
     * @var string
     */
    private $description;

    /**
     * @var string
     */
    private $servicekey;


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
     * Set activitykey
     *
     * @param string $activitykey
     * @return Usageactivities
     */
    public function setActivitykey($activitykey)
    {
        $this->activitykey = $activitykey;

        return $this;
    }

    /**
     * Get activitykey
     *
     * @return string 
     */
    public function getActivitykey()
    {
        return $this->activitykey;
    }

    /**
     * Set description
     *
     * @param string $description
     * @return Usageactivities
     */
    public function setDescription($description)
    {
        $this->description = $description;

        return $this;
    }

    /**
     * Get description
     *
     * @return string 
     */
    public function getDescription()
    {
        return $this->description;
    }

    /**
     * Set servicekey
     *
     * @param string $servicekey
     * @return Usageactivities
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
