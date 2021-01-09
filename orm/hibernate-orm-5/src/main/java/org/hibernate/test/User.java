package org.hibernate.test;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.util.Objects;

@Entity
public class User {

    @Id
    private String login;

    @LazyToOne(LazyToOneOption.NO_PROXY)
    @OneToOne(fetch = FetchType.LAZY)
    private Practitioner practitioner;

    public String getLogin() {
        return login;
    }

    public User setLogin(String login) {
        this.login = login;
        return this;
    }

    public Practitioner getPractitioner() {
        return practitioner;
    }

    public User setPractitioner(Practitioner practitioner) {
        this.practitioner = practitioner;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        return Objects.equals(getLogin(), other.getLogin());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLogin());
    }

}
