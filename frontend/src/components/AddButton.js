import React from 'react';
import styled, { css } from 'styled-components/macro';
import { BsPlus } from 'react-icons/bs';
import { useHistory } from 'react-router-dom';

export default function AddButton({ itemType, currentMonsterId }) {
  const history = useHistory();
  return (
    <AddButtonStyled
      onClick={createAddLink(currentMonsterId)}
      itemType={itemType}
    >
      <BsPlusStyled />
    </AddButtonStyled>
  );

  function createAddLink(monsterId) {
    if (itemType === 'monster') {
      return () => history.push('/monsters/create');
    }
    if (itemType === 'task') {
      return () => history.push('/monsters/' + monsterId + '/tasks/create');
    }
    if (itemType === 'reward')
      return () => history.push('/monsters/' + monsterId + '/rewards/create');
  }
}

const AddButtonStyled = styled.button`
  height: 50px;
  width: 50px;
  border: none;
  border-radius: 60px;
  background-color: var(--orange-main);
  position: absolute;
  bottom: 20px;
  right: 20px;
  box-shadow: var(--grey-shadow);

  ${(props) =>
    props.itemType === 'task' &&
    css`
      background-color: var(--blue-main);
    `}

  ${(props) =>
    props.itemType === 'reward' &&
    css`
      background-color: var(--green-main);
    `}
`;

const BsPlusStyled = styled(BsPlus)`
  color: white;
  height: var(--size-xl);
  width: var(--size-xl);
`;
