<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Internalusers
 */
class Internalusers
{
    /**
     * @var integer
     */
    private $id;

    /**
     * @var boolean
     */
    private $currentlyActive;

    /**
     * @var string
     */
    private $city;

    /**
     * @var string
     */
    private $country;

    /**
     * @var \DateTime
     */
    private $created;

    /**
     * @var string
     */
    private $createdBy;

    /**
     * @var string
     */
    private $department;

    /**
     * @var string
     */
    private $email;

    /**
     * @var string
     */
    private $fax;

    /**
     * @var string
     */
    private $firstName;

    /**
     * @var integer
     */
    private $gender;

    /**
     * @var string
     */
    private $institution;

    /**
     * @var string
     */
    private $lastName;

    /**
     * @var \DateTime
     */
    private $lastUpdated;

    /**
     * @var string
     */
    private $phone;

    /**
     * @var string
     */
    private $position;

    /**
     * @var string
     */
    private $researchArea;

    /**
     * @var string
     */
    private $state;

    /**
     * @var string
     */
    private $tenantId;

    /**
     * @var string
     */
    private $username;

    /**
     * @var string
     */
    private $uuid;

    /**
     * @var string
     */
    private $street1;

    /**
     * @var string
     */
    private $street2;


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
     * Set currentlyActive
     *
     * @param boolean $currentlyActive
     * @return Internalusers
     */
    public function setCurrentlyActive($currentlyActive)
    {
        $this->currentlyActive = $currentlyActive;

        return $this;
    }

    /**
     * Get currentlyActive
     *
     * @return boolean 
     */
    public function getCurrentlyActive()
    {
        return $this->currentlyActive;
    }

    /**
     * Set city
     *
     * @param string $city
     * @return Internalusers
     */
    public function setCity($city)
    {
        $this->city = $city;

        return $this;
    }

    /**
     * Get city
     *
     * @return string 
     */
    public function getCity()
    {
        return $this->city;
    }

    /**
     * Set country
     *
     * @param string $country
     * @return Internalusers
     */
    public function setCountry($country)
    {
        $this->country = $country;

        return $this;
    }

    /**
     * Get country
     *
     * @return string 
     */
    public function getCountry()
    {
        return $this->country;
    }

    /**
     * Set created
     *
     * @param \DateTime $created
     * @return Internalusers
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
     * Set createdBy
     *
     * @param string $createdBy
     * @return Internalusers
     */
    public function setCreatedBy($createdBy)
    {
        $this->createdBy = $createdBy;

        return $this;
    }

    /**
     * Get createdBy
     *
     * @return string 
     */
    public function getCreatedBy()
    {
        return $this->createdBy;
    }

    /**
     * Set department
     *
     * @param string $department
     * @return Internalusers
     */
    public function setDepartment($department)
    {
        $this->department = $department;

        return $this;
    }

    /**
     * Get department
     *
     * @return string 
     */
    public function getDepartment()
    {
        return $this->department;
    }

    /**
     * Set email
     *
     * @param string $email
     * @return Internalusers
     */
    public function setEmail($email)
    {
        $this->email = $email;

        return $this;
    }

    /**
     * Get email
     *
     * @return string 
     */
    public function getEmail()
    {
        return $this->email;
    }

    /**
     * Set fax
     *
     * @param string $fax
     * @return Internalusers
     */
    public function setFax($fax)
    {
        $this->fax = $fax;

        return $this;
    }

    /**
     * Get fax
     *
     * @return string 
     */
    public function getFax()
    {
        return $this->fax;
    }

    /**
     * Set firstName
     *
     * @param string $firstName
     * @return Internalusers
     */
    public function setFirstName($firstName)
    {
        $this->firstName = $firstName;

        return $this;
    }

    /**
     * Get firstName
     *
     * @return string 
     */
    public function getFirstName()
    {
        return $this->firstName;
    }

    /**
     * Set gender
     *
     * @param integer $gender
     * @return Internalusers
     */
    public function setGender($gender)
    {
        $this->gender = $gender;

        return $this;
    }

    /**
     * Get gender
     *
     * @return integer 
     */
    public function getGender()
    {
        return $this->gender;
    }

    /**
     * Set institution
     *
     * @param string $institution
     * @return Internalusers
     */
    public function setInstitution($institution)
    {
        $this->institution = $institution;

        return $this;
    }

    /**
     * Get institution
     *
     * @return string 
     */
    public function getInstitution()
    {
        return $this->institution;
    }

    /**
     * Set lastName
     *
     * @param string $lastName
     * @return Internalusers
     */
    public function setLastName($lastName)
    {
        $this->lastName = $lastName;

        return $this;
    }

    /**
     * Get lastName
     *
     * @return string 
     */
    public function getLastName()
    {
        return $this->lastName;
    }

    /**
     * Set lastUpdated
     *
     * @param \DateTime $lastUpdated
     * @return Internalusers
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
     * Set phone
     *
     * @param string $phone
     * @return Internalusers
     */
    public function setPhone($phone)
    {
        $this->phone = $phone;

        return $this;
    }

    /**
     * Get phone
     *
     * @return string 
     */
    public function getPhone()
    {
        return $this->phone;
    }

    /**
     * Set position
     *
     * @param string $position
     * @return Internalusers
     */
    public function setPosition($position)
    {
        $this->position = $position;

        return $this;
    }

    /**
     * Get position
     *
     * @return string 
     */
    public function getPosition()
    {
        return $this->position;
    }

    /**
     * Set researchArea
     *
     * @param string $researchArea
     * @return Internalusers
     */
    public function setResearchArea($researchArea)
    {
        $this->researchArea = $researchArea;

        return $this;
    }

    /**
     * Get researchArea
     *
     * @return string 
     */
    public function getResearchArea()
    {
        return $this->researchArea;
    }

    /**
     * Set state
     *
     * @param string $state
     * @return Internalusers
     */
    public function setState($state)
    {
        $this->state = $state;

        return $this;
    }

    /**
     * Get state
     *
     * @return string 
     */
    public function getState()
    {
        return $this->state;
    }

    /**
     * Set tenantId
     *
     * @param string $tenantId
     * @return Internalusers
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
     * Set username
     *
     * @param string $username
     * @return Internalusers
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
     * Set uuid
     *
     * @param string $uuid
     * @return Internalusers
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
     * Set street1
     *
     * @param string $street1
     * @return Internalusers
     */
    public function setStreet1($street1)
    {
        $this->street1 = $street1;

        return $this;
    }

    /**
     * Get street1
     *
     * @return string 
     */
    public function getStreet1()
    {
        return $this->street1;
    }

    /**
     * Set street2
     *
     * @param string $street2
     * @return Internalusers
     */
    public function setStreet2($street2)
    {
        $this->street2 = $street2;

        return $this;
    }

    /**
     * Get street2
     *
     * @return string 
     */
    public function getStreet2()
    {
        return $this->street2;
    }
}
