Request: {
  user: {
    name: string @tag(message="Name is required")
    age: int & >=18 & <=60 @tag(message="Age must be between 18 and 60")
  }
  contact: {
    email: string & =~"^.+@.+\\..+$" @tag(message="Invalid email format")
    phone: string & =~"^[0-9]{10}$" @tag(message="Phone must be 10 digits")
  }
}
